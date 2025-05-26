package com.three.recipingadsservicebe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.three.recipingadsservicebe.feign")
public class RecipingAdsServiceBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecipingAdsServiceBeApplication.class, args);
    }

}
