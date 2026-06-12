package com.faceai.pdfreader.model;

import java.util.List;

public record PdfContentResponse(
        String fileId,
        String fileName,
        int pageCount,
        String fileUrl,
        String fullText,
        List<PageText> pageTexts,
        List<ExtractedImage> images
) {
}
