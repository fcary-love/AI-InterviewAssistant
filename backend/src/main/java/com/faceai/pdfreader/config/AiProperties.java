package com.faceai.pdfreader.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(
        String baseUrl,
        String apiKey,
        String model,
        String textModel,
        String visionModel,
        String chatPath,
        Double temperature,
        Integer maxTokens
) {
}
