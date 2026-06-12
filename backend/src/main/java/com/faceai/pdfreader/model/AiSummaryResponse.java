package com.faceai.pdfreader.model;

public record AiSummaryResponse(
        String fileId,
        String fileName,
        String summary
) {
}
