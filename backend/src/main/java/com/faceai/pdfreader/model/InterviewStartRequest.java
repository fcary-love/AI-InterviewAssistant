package com.faceai.pdfreader.model;

public record InterviewStartRequest(
        String resumeFileId,
        String jdFileId,
        String jdText,
        String direction,
        String difficulty,
        String focus,
        String style,
        String questionMode,
        Boolean randomMix,
        Long interviewerId,
        Boolean adaptiveDifficulty
) {
}
