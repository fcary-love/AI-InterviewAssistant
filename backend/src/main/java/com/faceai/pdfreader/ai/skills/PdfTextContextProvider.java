package com.faceai.pdfreader.ai.skills;

import com.faceai.pdfreader.model.OcrResponse;
import com.faceai.pdfreader.model.PdfContentResponse;
import com.faceai.pdfreader.service.OcrService;
import com.faceai.pdfreader.service.PdfService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PdfTextContextProvider {

    private static final int MAX_TEXT_LENGTH = 12000;

    private final PdfService pdfService;
    private final OcrService ocrService;

    public PdfTextContextProvider(PdfService pdfService, OcrService ocrService) {
        this.pdfService = pdfService;
        this.ocrService = ocrService;
    }

    public PdfContentResponse loadContent(String fileId) {
        return pdfService.getContent(fileId);
    }

    public String getBestText(PdfContentResponse content) {
        String fullText = content.fullText();
        if (!StringUtils.hasText(fullText)) {
            OcrResponse ocrResponse = ocrService.extractTextFromPdf(content.fileId());
            fullText = ocrResponse.fullText();
        }
        if (!StringUtils.hasText(fullText)) {
            throw new IllegalArgumentException("当前 PDF 没有可用于 AI 分析的文本，请先执行 OCR 或上传可提取文本的 PDF");
        }
        if (fullText.length() <= MAX_TEXT_LENGTH) {
            return fullText;
        }
        return fullText.substring(0, MAX_TEXT_LENGTH) + "\n\n[内容过长，已截断前 " + MAX_TEXT_LENGTH + " 个字符用于分析]";
    }
}
