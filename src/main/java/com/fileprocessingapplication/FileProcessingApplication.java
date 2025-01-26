package com.fileprocessingapplication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.fileprocessingapplication.dao")
@ComponentScan(basePackages = {"com.fileprocessingapplication.service", "com.fileprocessingapplication.processor","com.fileprocessingapplication.messaging","com.fileprocessingapplication.util",
        "com.fileprocessingapplication.model","com.fileprocessingapplication.controller"})
public class FileProcessingApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileProcessingApplication.class, args);
    }

}
