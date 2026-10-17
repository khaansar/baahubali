package com.example.attemptservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AttemptServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AttemptServiceApplication.class, args);
    }
}
