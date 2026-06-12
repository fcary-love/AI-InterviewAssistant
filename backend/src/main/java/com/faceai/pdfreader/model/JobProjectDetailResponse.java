package com.faceai.pdfreader.model;

import java.util.List;

public record JobProjectDetailResponse(
        Long id,
        String companyName,
        String jobTitle,
        String jdText,
        Long resumeVersionId,
        String resumeFileId,
        String resumeFileName,
        Integer resumeVersionNo,
        Long matchAnalysisId,
        Integer matchScore,
        String status,
        String resumeSuggestions,
        String tailoredResumeText,
        String finalConclusion,
        String createdAt,
        String updatedAt,
        List<String> jdKeywords,
        List<String> resumeKeywords,
        List<String> missingKeywords
) {
}
