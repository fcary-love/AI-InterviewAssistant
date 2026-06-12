package com.faceai.pdfreader.service;

import com.faceai.pdfreader.ai.client.AiClient;
import com.faceai.pdfreader.ai.skills.ImageDescriptionSkill;
import com.faceai.pdfreader.ai.skills.PdfQaSkill;
import com.faceai.pdfreader.ai.skills.PdfSummarySkill;
import com.faceai.pdfreader.model.AiQaResponse;
import com.faceai.pdfreader.model.AiSummaryResponse;
import com.faceai.pdfreader.model.ChatHistoryMessage;
import com.faceai.pdfreader.model.ImageContentResponse;
import com.faceai.pdfreader.model.ImageDescriptionResponse;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AiService {

    private final AiClient aiClient;
    private final PdfSummarySkill pdfSummarySkill;
    private final PdfQaSkill pdfQaSkill;
    private final ImageDescriptionSkill imageDescriptionSkill;
    private final ImageService imageService;

    public AiService(
            AiClient aiClient,
            PdfSummarySkill pdfSummarySkill,
            PdfQaSkill pdfQaSkill,
            ImageDescriptionSkill imageDescriptionSkill,
            ImageService imageService
    ) {
        this.aiClient = aiClient;
        this.pdfSummarySkill = pdfSummarySkill;
        this.pdfQaSkill = pdfQaSkill;
        this.imageDescriptionSkill = imageDescriptionSkill;
        this.imageService = imageService;
    }

    public AiSummaryResponse summarize(String fileId) {
        aiClient.validateConfiguration();
        return pdfSummarySkill.execute(fileId);
    }

    public AiQaResponse answerQuestion(String fileId, String question, List<ChatHistoryMessage> history) {
        aiClient.validateConfiguration();
        return pdfQaSkill.execute(fileId, question, history);
    }

    public ImageDescriptionResponse describeImage(String fileId, String imageUrl) {
        aiClient.validateConfiguration();
        return imageDescriptionSkill.execute(fileId, imageUrl);
    }

    public ImageDescriptionResponse describeUploadedImage(String fileId) {
        aiClient.validateConfiguration();
        ImageContentResponse content = imageService.getContent(fileId);
        return imageDescriptionSkill.execute(fileId, content.fileUrl());
    }
}
