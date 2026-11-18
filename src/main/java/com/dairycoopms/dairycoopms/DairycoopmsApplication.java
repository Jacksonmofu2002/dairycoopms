package com.dairycoopms.dairycoopms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class DairycoopmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(DairycoopmsApplication.class, args);
    }

}