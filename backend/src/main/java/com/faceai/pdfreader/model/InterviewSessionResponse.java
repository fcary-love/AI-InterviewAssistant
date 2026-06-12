package com.faceai.pdfreader.model;

public record InterviewSessionResponse(
        String sessionId,
        String status,
        String summary,
        String firstQuestion,
        String resumeText,
        String jdText,
        String candidateProfile,
        String currentDifficulty
) {
    public InterviewSessionResponse(String sessionId, String status, String summary, String firstQuestion) {
        this(sessionId, status, summary, firstQuestion, null, null, null, null);
    }

    public InterviewSessionResponse(String sessionId, String status, String summary, String firstQuestion,
                                     String resumeText, String jdText, String candidateProfile) {
        this(sessionId, status, summary, firstQuestion, resumeText, jdText, candidateProfile, null);
    }
}
