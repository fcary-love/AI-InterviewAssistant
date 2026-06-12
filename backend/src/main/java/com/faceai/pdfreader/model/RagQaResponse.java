package com.faceai.pdfreader.model;

import java.util.List;

public record RagQaResponse(
        String fileId,
        String fileName,
        String question,
        String answer,
        List<RagReferenceChunk> references
) {
}
