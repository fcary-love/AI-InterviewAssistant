package com.faceai.pdfreader.model;

public record InterviewReportSummaryResponse(
        String sessionId,
        Integer totalScore,
        String summary,
        String updatedAt,
        String scores
) {
    public InterviewReportSummaryResponse(String sessionId, Integer totalScore, String summary, String updatedAt) {
        this(sessionId, totalScore, summary, updatedAt, null);
    }
}
