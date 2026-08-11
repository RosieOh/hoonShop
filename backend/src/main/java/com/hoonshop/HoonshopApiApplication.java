package com.hoonshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
// 결제 대사(reconciliation) 배치를 위해 필요합니다.
@EnableScheduling
public class HoonshopApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(HoonshopApiApplication.class, args);
    }
}
