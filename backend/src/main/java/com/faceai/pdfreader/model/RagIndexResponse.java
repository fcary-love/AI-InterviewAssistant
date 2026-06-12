package com.faceai.pdfreader.model;

public record RagIndexResponse(
        String fileId,
        String fileName,
        int chunkCount,
        String message
) {
}
