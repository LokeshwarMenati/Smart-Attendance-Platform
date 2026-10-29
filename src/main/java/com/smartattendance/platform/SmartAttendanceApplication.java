package com.smartattendance.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SmartAttendanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartAttendanceApplication.class, args);
    }
}

