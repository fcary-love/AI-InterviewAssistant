package com.faceai.pdfreader.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AiConfig {

    @Bean
    public WebClient aiWebClient(WebClient.Builder builder, AiProperties aiProperties) {
        return builder
                .baseUrl(aiProperties.baseUrl())
                .defaultHeader("Authorization", "Bearer " + aiProperties.apiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
