package com.antra.report.client;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

//@EnableFeignClients //start Feign client  //eureka+feign+ribbon
//@EnableRetry
@EnableCircuitBreaker //start Hystrix
@EnableDiscoveryClient //start service registeration and discovery
@SpringBootApplication
public class MainClientApplication {

  //eureka+ribbon+resttemplate
  @LoadBalanced
  @Bean
  public RestTemplate restTemplate(){
	  /*set timeout 
	  HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
	  clientHttpRequestFactory.setConnectTimeout(3000);
	  return new RestTemplate(clientHttpRequestFactory);
      */
      return new RestTemplate();
  }



  @Bean
  public QueueMessagingTemplate queueMessagingTemplate(
          AmazonSQSAsync amazonSQSAsync) {
      return new QueueMessagingTemplate(amazonSQSAsync);
  }
  public static void main(String[] args) {
      SpringApplication.run(MainClientApplication.class, args);
  }

}
