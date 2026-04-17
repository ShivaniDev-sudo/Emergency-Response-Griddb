package com.griddb.emergency;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EmergencyApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmergencyApplication.class, args);
    }
}
