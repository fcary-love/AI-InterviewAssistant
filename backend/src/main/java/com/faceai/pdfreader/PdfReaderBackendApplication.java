package com.faceai.pdfreader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PdfReaderBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(PdfReaderBackendApplication.class, args);
    }
}
