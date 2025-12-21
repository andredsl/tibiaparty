package com.tibia.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TibiaApplication {

    public static void main(String[] args) {
        SpringApplication.run(TibiaApplication.class, args);
    }
}
