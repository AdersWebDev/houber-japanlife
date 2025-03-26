package com.lee.osakacity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class OsakaCityApplication {

    public static void main(String[] args) {
        SpringApplication.run(OsakaCityApplication.class, args);
    }

}
