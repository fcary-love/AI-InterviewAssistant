package com.faceai.pdfreader.model;

public record InterviewAnswerRequest(
        String answer,
        Integer durationSeconds
) {
}
