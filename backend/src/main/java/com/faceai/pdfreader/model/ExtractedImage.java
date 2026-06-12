package com.faceai.pdfreader.model;

public record ExtractedImage(
        int pageNumber,
        String imageName,
        String imageUrl
) {
}
