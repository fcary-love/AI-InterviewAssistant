package com.faceai.pdfreader.model;

public record JobProjectSummaryResponse(
        Long id,
        String companyName,
        String jobTitle,
        String status,
        Integer matchScore,
        String resumeFileName,
        String jdExcerpt,
        String updatedAt
) {
}
