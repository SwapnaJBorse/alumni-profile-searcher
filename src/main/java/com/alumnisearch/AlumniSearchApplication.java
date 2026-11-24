package com.alumnisearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AlumniSearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlumniSearchApplication.class, args);
    }
}
