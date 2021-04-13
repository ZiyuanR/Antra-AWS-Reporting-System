package com.antra.report.client.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.antra.report.client.pojo.reponse.PDFResponse;

//eureka+feign+ribbon
@FeignClient("PDF-SERVICE")
public interface PDFServiceFeign{
    @PostMapping("/pdf")
    public ResponseEntity<PDFResponse> createPDF(@RequestBody @Validated PDFRequest request);
}
