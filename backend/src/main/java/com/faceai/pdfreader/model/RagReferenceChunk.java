package com.faceai.pdfreader.model;

public record RagReferenceChunk(
        String text,
        int pageNumber,
        int chunkIndex
) {
}
