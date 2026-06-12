package com.faceai.pdfreader.model;

public record ImageContentResponse(
        String fileId,
        String fileName,
        String fileUrl
) {
}
