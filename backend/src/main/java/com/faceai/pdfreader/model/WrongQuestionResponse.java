package com.faceai.pdfreader.model;

public record WrongQuestionResponse(
        String sessionId,
        Integer questionNo,
        String question,
        String answer,
        Integer score,
        String aiComment,
        Integer durationSeconds,
        String createdAt,
        Boolean reviewed
) {
}
