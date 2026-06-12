package com.faceai.pdfreader.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ocr")
public record OcrProperties(
        boolean enabled,
        String dataPath,
        String language,
        int renderDpi,
        String imageFormat,
        String pageImageDir
) {
}
