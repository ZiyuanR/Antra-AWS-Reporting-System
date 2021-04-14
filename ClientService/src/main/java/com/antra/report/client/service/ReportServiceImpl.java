package com.antra.report.client.service;

import com.amazonaws.services.s3.AmazonS3;
import com.antra.report.client.entity.ExcelReportEntity;
import com.antra.report.client.entity.PDFReportEntity;
import com.antra.report.client.entity.ReportRequestEntity;
import com.antra.report.client.entity.ReportStatus;
import com.antra.report.client.exception.RequestNotFoundException;
import com.antra.report.client.pojo.EmailType;
import com.antra.report.client.pojo.FileType;
import com.antra.report.client.pojo.reponse.ExcelResponse;
import com.antra.report.client.pojo.reponse.PDFResponse;
import com.antra.report.client.pojo.reponse.ReportVO;
import com.antra.report.client.pojo.reponse.SqsResponse;
import com.antra.report.client.pojo.request.ReportRequest;
import com.antra.report.client.repository.ReportRequestRepo;
import com.antra.report.client.util.SmallTool;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {
    private static final Logger log = LoggerFactory.getLogger(ReportServiceImpl.class);

    private final ReportRequestRepo reportRequestRepo;
    private final SNSService snsService;
    private final AmazonS3 s3Client;
    private final EmailService emailService;
    private final RestTemplate rs;
    
    //eureka+ribbon+resttemplate
    public ReportServiceImpl(ReportRequestRepo reportRequestRepo, SNSService snsService, AmazonS3 s3Client, EmailService emailService, RestTemplate rs) {
        this.reportRequestRepo = reportRequestRepo;
        this.snsService = snsService;
        this.s3Client = s3Client;
        this.emailService = emailService;
        this.rs = rs;
    }

    private ReportRequestEntity persistToLocal(ReportRequest request) {
        request.setReqId("Req-"+ UUID.randomUUID().toString());

        ReportRequestEntity entity = new ReportRequestEntity();
        entity.setReqId(request.getReqId());
        entity.setSubmitter(request.getSubmitter());
        entity.setDescription(request.getDescription());
        entity.setCreatedTime(LocalDateTime.now());

        PDFReportEntity pdfReport = new PDFReportEntity();
        pdfReport.setRequest(entity);
        pdfReport.setStatus(ReportStatus.PENDING);
        pdfReport.setCreatedTime(LocalDateTime.now());
        entity.setPdfReport(pdfReport);

        ExcelReportEntity excelReport = new ExcelReportEntity();
        BeanUtils.copyProperties(pdfReport, excelReport);
        entity.setExcelReport(excelReport);

        return reportRequestRepo.save(entity);
    }

    @Override
    public ReportVO generateReportsSync(ReportRequest request) {
        persistToLocal(request);
        sendDirectRequests(request);
        return new ReportVO(reportRequestRepo.findById(request.getReqId()).orElseThrow());
    }
    
    
    
  //TODO:Change to parallel process using Threadpool? CompletableFuture?
    @HystrixCommand(fallbackMethod="sendDirectRequestsByHystrix",
    		commandProperties= {
    				@HystrixProperty(name="execution.isolation.thread.timeoutInMilliseconds", value="3000")
    		})
    private void sendDirectRequests(ReportRequest request) {
        //eureka+ribbon+resttemplate

        //RestTemplate rs = new RestTemplate();
        ExcelResponse excelResponse = new ExcelResponse();
        PDFResponse pdfResponse = new PDFResponse();
        ExecutorService executor = Executors.newCachedThreadPool();
        //thenCombine(CompletionStage, BiFunction)
        //call PDF and excel service->
        //Rest: 1. Ribbon-RestTemplate 2. Feign
        //move to application -(loadbalanced)
        CompletableFuture<ExcelResponse> excelFuture = CompletableFuture.supplyAsync(()->{
            SmallTool.printTimeAndThread("excelFuture");
            //excelResponse = rs.postForEntity("http://localhost:8888/excel", request, ExcelResponse.class).getBody();
            // in case of later cluster
            excelResponse = rs.postForEntity("http://EXCEL-SERVICE/excel", request, ExcelResponse.class).getBody();
            return excelResponse;
        }, executor);
        CompletableFuture<PDFResponse> pdfFuture = CompletableFuture.supplyAsync(()->{
            SmallTool.printTimeAndThread("pdfFuture");
            //pdfResponse = rs.postForEntity("http://localhost:9999/pdf", request, PDFResponse.class).getBody();
            pdfResponse = rs.postForEntity("http://PDF-SERVICE/pdf", request, PDFResponse.class).getBody();
            return pdfResponse;
        }, executor);
        excelFuture.thenAcceptBoth(pdfFuture, (er, pr)->{});
        excelFuture.join();
        executor.shutdown();
    }
    private void sendDirectRequestsByHystrix(ReportRequest request) {
        ExcelResponse excelResponse = new ExcelResponse();
        PDFResponse pdfResponse = new PDFResponse();
        ExecutorService executor = Executors.newCachedThreadPool();
        CompletableFuture<ExcelResponse> excelFuture = CompletableFuture.supplyAsync(()->{
            SmallTool.printTimeAndThread("excelFuture");
            return excelResponse;
        }, executor);
        CompletableFuture<PDFResponse> pdfFuture = CompletableFuture.supplyAsync(()->{
            SmallTool.printTimeAndThread("pdfFuture");
            return pdfResponse;
        }, executor);
        excelFuture.thenAcceptBoth(pdfFuture, (er, pr)->{});
        excelFuture.join();
        executor.shutdown();
    }
    
    private void updateLocal(ExcelResponse excelResponse) {
        SqsResponse response = new SqsResponse();
        BeanUtils.copyProperties(excelResponse, response);
        updateAsyncExcelReport(response);
    }
    private void updateLocal(PDFResponse pdfResponse) {
        SqsResponse response = new SqsResponse();
        BeanUtils.copyProperties(pdfResponse, response);
        updateAsyncPDFReport(response);
    }

    @Override
    @Transactional
    public ReportVO generateReportsAsync(ReportRequest request) {
        ReportRequestEntity entity = persistToLocal(request);
        snsService.sendReportNotification(request);
        log.info("Send SNS the message: {}",request);
        return new ReportVO(entity);
    }

    @Override
//    @Transactional // why this? email could fail
    public void updateAsyncPDFReport(SqsResponse response) {
        ReportRequestEntity entity = reportRequestRepo.findById(response.getReqId()).orElseThrow(RequestNotFoundException::new);
        var pdfReport = entity.getPdfReport();
        pdfReport.setUpdatedTime(LocalDateTime.now());
        if (response.isFailed()) {
            pdfReport.setStatus(ReportStatus.FAILED);
        } else{
            pdfReport.setStatus(ReportStatus.COMPLETED);
            pdfReport.setFileId(response.getFileId());
            pdfReport.setFileLocation(response.getFileLocation());
            pdfReport.setFileSize(response.getFileSize());
        }
        entity.setUpdatedTime(LocalDateTime.now());
        reportRequestRepo.save(entity);
        String to = "youremail@gmail.com";
        emailService.sendEmail(to, EmailType.SUCCESS, entity.getSubmitter());
    }

    @Override
//    @Transactional
    public void updateAsyncExcelReport(SqsResponse response) {
        ReportRequestEntity entity = reportRequestRepo.findById(response.getReqId()).orElseThrow(RequestNotFoundException::new);
        var excelReport = entity.getExcelReport();
        excelReport.setUpdatedTime(LocalDateTime.now());
        if (response.isFailed()) {
            excelReport.setStatus(ReportStatus.FAILED);
        } else{
            excelReport.setStatus(ReportStatus.COMPLETED);
            excelReport.setFileId(response.getFileId());
            excelReport.setFileLocation(response.getFileLocation());
            excelReport.setFileSize(response.getFileSize());
        }
        entity.setUpdatedTime(LocalDateTime.now());
        reportRequestRepo.save(entity);
        String to = "youremail@gmail.com";
        emailService.sendEmail(to, EmailType.SUCCESS, entity.getSubmitter());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportVO> getReportList() {
        return reportRequestRepo.findAll().stream().map(ReportVO::new).collect(Collectors.toList());
    }

    @Override
    public InputStream getFileBodyByReqId(String reqId, FileType type) {
        ReportRequestEntity entity = reportRequestRepo.findById(reqId).orElseThrow(RequestNotFoundException::new);
        if (type == FileType.PDF) {
            String fileLocation = entity.getPdfReport().getFileLocation(); // this location is s3 "bucket/key"
            String bucket = fileLocation.split("/")[0];
            String key = fileLocation.split("/")[1];
            return s3Client.getObject(bucket, key).getObjectContent();
        } else if (type == FileType.EXCEL) {
            String fileId = entity.getExcelReport().getFileId();
//            String fileLocation = entity.getExcelReport().getFileLocation();
//            try {
//                return new FileInputStream(fileLocation);// this location is in local, definitely sucks
//            } catch (FileNotFoundException e) {
//                log.error("No file found", e);
//            }
            RestTemplate restTemplate = new RestTemplate();
//            InputStream is = restTemplate.execute(, HttpMethod.GET, null, ClientHttpResponse::getBody, fileId);
            ResponseEntity<Resource> exchange = restTemplate.exchange("http://localhost:8888/excel/{id}/content",
                    HttpMethod.GET, null, Resource.class, fileId);
            try {
                return exchange.getBody().getInputStream();
            } catch (IOException e) {
                log.error("Cannot download excel",e);
            }
        }
        return null;
    }
    
    //delete function
    @Override
    public void removeFile(String reqId) {
        sendDeleteID(reqId);
        reportRequestRepo.deleteById(reqId);
    }
    private void sendDeleteID(String redId) {
        //eureka+ribbon+resttemplate
        ExcelResponse excelResponse = new ExcelResponse();
        PDFResponse pdfResponse = new PDFResponse();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CompletableFuture<ExcelResponse> excelFuture = CompletableFuture.runAsync(()->{
            SmallTool.printTimeAndThread("excelFutureDelete");
            // in case of later cluster
            final String uri = "http://EXCEL-SERVICE/excel/{id}";
            Map<String, String> params = new HashMap<String, String>();
            params.put("id", reqId);
            rs.delete (uri, params);
        }, executor);
        CompletableFuture<PDFResponse> pdfFuture = CompletableFuture.runAsync(()->{
            SmallTool.printTimeAndThread("pdfFutureDelete");
            final String uri = "http://PDF-SERVICE/pdf/{id}";
            Map<String, String> params = new HashMap<String, String>();
            params.put("id", reqId);
            rs.delete (uri, params);
        }, executor);
        excelFuture.runAfterBoth(pdfFuture, ()->{});
        excelFuture.join();
        executor.shutdown();
    }

    @Override
    public ReportRequestEntity findFileById(String id) {
		ReportRequestEntity entity = reportRequestRepo.findById(id).orElseThrow(RequestNotFoundException::new);
		return entity;
	}
}
