package com.faceai.pdfreader.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        String rootDir,
        String uploadDir,
        String documentUploadDir,
        String imageUploadDir,
        String imageDir,
        long maxFileSizeMb
) {
}
