package com.stattracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class StatTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(StatTrackerApplication.class, args);
    }
}
