package com.faceai.pdfreader.model;

import java.time.LocalDateTime;

public record DocumentRecord(
        Long id,
        String fileId,
        String fileName,
        String fileType,
        String fileUrl,
        String fullText,
        LocalDateTime createdAt
) {
}
