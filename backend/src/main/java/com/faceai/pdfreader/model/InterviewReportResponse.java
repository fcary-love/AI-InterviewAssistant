package com.faceai.pdfreader.model;

public record InterviewReportResponse(
        String sessionId,
        Integer totalScore,
        String reportContent,
        String userReflection,
        String summary,
        String updatedAt,
        String scores,
        String strengths,
        String weaknesses,
        String advice
) {
    public InterviewReportResponse(
            String sessionId, Integer totalScore, String reportContent,
            String userReflection, String summary, String updatedAt
    ) {
        this(sessionId, totalScore, reportContent, userReflection, summary, updatedAt, null, null, null, null);
    }
}
