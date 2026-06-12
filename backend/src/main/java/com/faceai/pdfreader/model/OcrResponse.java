package com.faceai.pdfreader.model;

import java.util.List;

public record OcrResponse(
        String fileId,
        String fileName,
        int pageCount,
        String fullText,
        List<OcrPageResult> pageResults
) {
}
