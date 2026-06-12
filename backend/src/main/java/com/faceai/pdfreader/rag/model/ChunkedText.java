package com.faceai.pdfreader.rag.model;

public record ChunkedText(
        String id,
        String text,
        ChunkMetadata metadata
) {
}
