package com.faceai.pdfreader.model;

import java.util.Map;

public record InterviewTurnResponse(
        String sessionId,
        Integer questionNo,
        String question,
        String answer,
        Integer durationSeconds,
        String aiComment,
        Integer score,
        Map<String, Integer> scores,
        Boolean followUp,
        String nextQuestion,
        Boolean finished
) {
    public InterviewTurnResponse(
            String sessionId, Integer questionNo, String question, String answer,
            Integer durationSeconds, String aiComment, Integer score,
            String nextQuestion, Boolean finished
    ) {
        this(sessionId, questionNo, question, answer, durationSeconds, aiComment, score, null, null, nextQuestion, finished);
    }
}
