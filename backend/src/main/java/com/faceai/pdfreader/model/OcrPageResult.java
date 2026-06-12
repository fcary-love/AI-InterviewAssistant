package com.faceai.pdfreader.model;

public record OcrPageResult(
        int pageNumber,
        String text,
        String imageUrl
) {
}
