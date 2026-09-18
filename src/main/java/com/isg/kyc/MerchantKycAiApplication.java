package com.isg.kyc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableKafka
public class MerchantKycAiApplication {
    public static void main(String[] args) {
        SpringApplication.run(MerchantKycAiApplication.class, args);
    }
}
