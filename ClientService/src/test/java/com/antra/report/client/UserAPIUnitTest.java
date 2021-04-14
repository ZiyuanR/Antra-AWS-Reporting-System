package com.antra.report.client;


import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.util.ArrayList;
import java.util.List;

import com.antra.report.client.controller.ReportController;
import com.antra.report.client.entity.ReportRequestEntity;
import com.antra.report.client.exception.RequestNotFoundException;
import com.antra.report.client.pojo.reponse.ReportVO;
import com.antra.report.client.pojo.request.ReportRequest;
import com.antra.report.client.service.ReportService;
import com.antra.report.client.service.SNSService;


public class UserAPIUnitTest {

    @Mock
    ReportService reportService;


    @Before
    public void configMock() {
        MockitoAnnotations.initMocks(this);
        RestAssuredMockMvc.standaloneSetup(new ReportController(reportService));
        //Mockito.when(messages.getMessage(anyObject())).thenReturn("Mocked Message");
    }



    @Test
    public void testDeleteReportButExceptionRaised(){
    	Mockito.when(reportService.removeFile(String.valueOf(anyInt()))).thenThrow(new RequestNotFoundException("The file you want to delete is not found"));
        given().accept("application/json").get("/report/11").peek().
                then().assertThat()
                .statusCode(404)
                .body("message",Matchers.is("The file you want to delete is not found"));
    }


    @Test
    public void testListReport(){
        ReportRequestEntity request = new ReportRequestEntity();
		request.setSubmitter("jr_test");
		request.setDescription("This is just a test");
		ReportRequestEntity request2 = new ReportRequestEntity();
		request2.setSubmitter("jr_test2");
        request2.setDescription("This is just a test2");
        
        ReportVO vo1 = new ReportVO(request);
		ReportVO vo2 = new ReportVO(request2);
		List<ReportVO> list = new ArrayList<ReportVO>();
		list.add(vo1);
		list.add(vo2);
        Mockito.when(reportService.getReportList()).thenReturn(list);
        List<ReportVO> voList = reportService.getReportList();
        assertThat(voList.size(), is(2));
        assertThat(voList, containsInAnyOrder(
                hasProperty(ReportRequestEntity, is(request)),
                hasProperty(ReportRequestEntity, is(request2))
        ));
    }
}


