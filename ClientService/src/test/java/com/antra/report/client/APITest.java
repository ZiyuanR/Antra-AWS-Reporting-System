package com.antra.report.client;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class UserAPITest {

    @Value("http://localhost:${local.server.port}")
    private String REST_SERVICE_URI ;



    @Test
    public void deleteNoneExistingFileByID(){
        when().
                get(REST_SERVICE_URI + "/report/100").peek().
                then().assertThat()
                .statusCode(404)
                .body("errorCode",Matchers.equalTo(404));
    }
}
