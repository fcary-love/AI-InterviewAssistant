package com.faceai.pdfreader.ai.skills;

import com.faceai.pdfreader.ai.client.AiClient;
import com.faceai.pdfreader.ai.prompts.PromptTemplates;
import com.faceai.pdfreader.model.AiQaResponse;
import com.faceai.pdfreader.model.ChatHistoryMessage;
import com.faceai.pdfreader.model.PdfContentResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PdfQaSkill {

    private final AiClient aiClient;
    private final PdfTextContextProvider textContextProvider;

    public PdfQaSkill(AiClient aiClient, PdfTextContextProvider textContextProvider) {
        this.aiClient = aiClient;
        this.textContextProvider = textContextProvider;
    }

    public AiQaResponse execute(String fileId, String question, List<ChatHistoryMessage> history) {
        PdfContentResponse content = textContextProvider.loadContent(fileId);
        String prompt = PromptTemplates.qaPrompt(textContextProvider.getBestText(content), question, history);
        String answer = aiClient.chat(prompt);
        return new AiQaResponse(content.fileId(), content.fileName(), question, answer);
    }
}
