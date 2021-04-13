package com.antra.report.client.service;

import java.io.FileNotFoundException;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.antra.report.client.pojo.reponse.ExcelResponse;

//eureka+feign+ribbon
@FeignClient("EXCEL-SERVICE")
public interface ExcelServiceFeign{

    @DeleteMapping("/excel/{id}")
    public ResponseEntity<ExcelResponse> deleteExcel(@PathVariable String id) throws FileNotFoundException
}