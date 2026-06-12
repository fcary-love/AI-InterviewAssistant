package com.faceai.pdfreader.model;

public record AiQaResponse(
        String fileId,
        String fileName,
        String question,
        String answer
) {
}
