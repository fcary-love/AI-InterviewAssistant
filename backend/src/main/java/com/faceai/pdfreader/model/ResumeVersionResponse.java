package com.faceai.pdfreader.model;

public record ResumeVersionResponse(
        Long id,
        String fileId,
        String fileName,
        String fileType,
        Integer versionNo,
        Integer textLength,
        String skillKeywords,
        String contentPreview,
        String createdAt
) {
}
