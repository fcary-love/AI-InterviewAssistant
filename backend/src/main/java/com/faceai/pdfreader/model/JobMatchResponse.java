package com.faceai.pdfreader.model;

import java.util.List;

public record JobMatchResponse(
        Long id,
        String resumeFileId,
        Integer matchScore,
        String analysisContent,
        String createdAt,
        List<String> coreRequirements,
        List<String> matchedKeywords,
        List<String> missingKeywords,
        List<String> rewriteSuggestions,
        List<String> interviewFocus
) {
    public JobMatchResponse(
            Long id,
            String resumeFileId,
            Integer matchScore,
            String analysisContent,
            String createdAt
    ) {
        this(id, resumeFileId, matchScore, analysisContent, createdAt, List.of(), List.of(), List.of(), List.of(), List.of());
    }
}
