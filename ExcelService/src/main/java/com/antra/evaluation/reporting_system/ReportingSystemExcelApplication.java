package com.antra.evaluation.reporting_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableDiscoveryClient //start service registeration and discovery
@SpringBootApplication
public class ReportingSystemExcelApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReportingSystemExcelApplication.class, args);
    }

}
