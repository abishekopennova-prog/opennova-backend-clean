package com.opennova;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class OpenNovaApplication {
    public static void main(String[] args) {
        SpringApplication.run(OpenNovaApplication.class, args);
    }
}