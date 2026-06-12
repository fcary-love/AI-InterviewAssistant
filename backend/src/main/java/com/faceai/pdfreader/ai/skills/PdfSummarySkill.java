package com.faceai.pdfreader.ai.skills;

import com.faceai.pdfreader.ai.client.AiClient;
import com.faceai.pdfreader.ai.prompts.PromptTemplates;
import com.faceai.pdfreader.model.AiSummaryResponse;
import com.faceai.pdfreader.model.PdfContentResponse;
import org.springframework.stereotype.Component;

@Component
public class PdfSummarySkill {

    private final AiClient aiClient;
    private final PdfTextContextProvider textContextProvider;

    public PdfSummarySkill(AiClient aiClient, PdfTextContextProvider textContextProvider) {
        this.aiClient = aiClient;
        this.textContextProvider = textContextProvider;
    }

    public AiSummaryResponse execute(String fileId) {
        PdfContentResponse content = textContextProvider.loadContent(fileId);
        String prompt = PromptTemplates.summaryPrompt(textContextProvider.getBestText(content));
        String summary = aiClient.chat(prompt);
        return new AiSummaryResponse(content.fileId(), content.fileName(), summary);
    }
}
