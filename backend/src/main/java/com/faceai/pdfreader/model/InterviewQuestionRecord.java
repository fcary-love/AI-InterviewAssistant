package com.faceai.pdfreader.model;

public record InterviewQuestionRecord(
        Long id,
        String direction,
        String category,
        String difficulty,
        String questionText,
        String referenceAnswer,
        String sourceName,
        String sourceUrl
) {
}
