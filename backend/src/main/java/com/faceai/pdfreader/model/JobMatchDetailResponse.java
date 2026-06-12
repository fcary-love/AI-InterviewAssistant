package com.faceai.pdfreader.model;

import java.util.List;

public record JobMatchDetailResponse(
        Long id,
        String resumeFileId,
        String resumeFileName,
        Integer matchScore,
        String jdText,
        String analysisContent,
        String createdAt,
        List<String> coreRequirements,
        List<String> matchedKeywords,
        List<String> missingKeywords,
        List<String> rewriteSuggestions,
        List<String> interviewFocus
) {
}
