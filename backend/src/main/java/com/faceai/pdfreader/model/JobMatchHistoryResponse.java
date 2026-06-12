package com.faceai.pdfreader.model;

public record JobMatchHistoryResponse(
        Long id,
        String resumeFileId,
        Integer matchScore,
        String jdExcerpt,
        String createdAt
) {
}
