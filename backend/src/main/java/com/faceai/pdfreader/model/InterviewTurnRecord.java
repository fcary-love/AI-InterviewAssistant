package com.faceai.pdfreader.model;

public record InterviewTurnRecord(
        String sessionId,
        Integer questionNo,
        String question,
        String answer,
        Integer durationSeconds,
        String aiComment,
        Integer score,
        String scores,
        Boolean followUp
) {
    public InterviewTurnRecord(
            String sessionId, Integer questionNo, String question, String answer,
            Integer durationSeconds, String aiComment, Integer score
    ) {
        this(sessionId, questionNo, question, answer, durationSeconds, aiComment, score, null, null);
    }
}
