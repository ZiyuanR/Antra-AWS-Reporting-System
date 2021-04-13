# Antra SEP java evaluation project
## 1. Setup the environment and make it run.
 All three projects are Sprintboot application.<br>

 Need to setup AWS SNS/SQS/S3 in order to use the async API.(Videos in LMS)<br>

 Make sure to update your <i>application.properties</i> file with your AWS IAM account secrets and region.(Videos in LMS)

 AWS Lambda(Sending email) is optional. Code is in [sendEmailCode.py](./lambda/sendEmailCode.py)

## 2. Understand the structure and details
Look at the [ReportingSystemArchitecture.pdf](./ReportingSystemArchitecture.pdf)

Look at the [EurekaRibbon.pdf](./EurekaRibbon.pdf)


## 3. Make improvement in the code/system level.

1. Implement delete API(delete in both the ClientService and Excel/PDF service) in ClientService
2. Add more exception
2. Improve sync API performance by using multithreading and sending request concurrently to both services.
3. Use a database instead of hashmap in the ExcelRepositoryImpl.
4. Add Unit/Integeration tests for ClientService
5. Convert sync API into microservices by adding Eureka/Ribbon(RestTemplate and Feign) support.
6. Supply fault tolerance with Hystrix(fallback, timeout)




## 4. Send your code to [Dawei Zhuang(dawei.zhuang@antra.com)](dawei.zhuang@antra.com) using Github/Gitlab. 
Make sure there is README.MD to indicate what did you change/add to the project.

