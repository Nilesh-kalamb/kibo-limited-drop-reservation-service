package com.kibo.drop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LimitedDropApplication {
    public static void main(String[] args) {
        SpringApplication.run(LimitedDropApplication.class, args);
    }
}
