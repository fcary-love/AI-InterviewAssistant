package com.faceai.pdfreader.model;

public record DocumentUploadResponse(
        String fileId,
        String fileName,
        String fileType,
        String fileUrl,
        String fullText
) {
}
