package com.faceai.pdfreader.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rag")
public record RagProperties(
        boolean enabled,
        int chunkSize,
        int chunkOverlap,
        int topK,
        double similarityThreshold,
        String indexPrefix
) {
}
