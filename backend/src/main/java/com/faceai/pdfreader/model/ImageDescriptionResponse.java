package com.faceai.pdfreader.model;

public record ImageDescriptionResponse(
        String fileId,
        String imageUrl,
        String description
) {
}
