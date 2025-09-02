package com.example.emr_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableJpaRepositories(basePackages = "com.example.emr_server.repository")
@EnableScheduling
@SpringBootApplication
public class EmrServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmrServerApplication.class, args);
    }

}
