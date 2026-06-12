package com.faceai.pdfreader.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.faceai.pdfreader.ai.client.AiClient;
import com.faceai.pdfreader.model.DocumentQaResponse;
import com.faceai.pdfreader.model.DocumentSummaryResponse;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentAiServiceTest {

    @Mock
    private AiClient aiClient;
    @Mock
    private DocumentService documentService;

    @InjectMocks
    private DocumentAiService documentAiService;

    @Test
    void summarize_callsAiClient_withDocumentText() {
        when(documentService.readText("file123")).thenReturn("这是简历内容");
        when(aiClient.chat(anyString())).thenReturn("这是一份Java开发者的简历");

        DocumentSummaryResponse response = documentAiService.summarize("file123");

        assertNotNull(response);
        assertEquals("这是一份Java开发者的简历", response.summary());
    }

    @Test
    void summarize_throwsWhenNoText() {
        when(documentService.readText("file123")).thenReturn("");

        assertThrows(IllegalArgumentException.class,
                () -> documentAiService.summarize("file123"));
    }

    @Test
    void summarize_throwsWhenTextIsNull() {
        when(documentService.readText("file123")).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> documentAiService.summarize("file123"));
    }

    @Test
    void answerQuestion_includesQuestionInPrompt() {
        when(documentService.readText("file123")).thenReturn("简历内容");
        when(aiClient.chat(anyString())).thenReturn("回答内容");

        DocumentQaResponse response = documentAiService.answerQuestion(
                "file123", "如何优化简历？", Collections.emptyList()
        );

        assertNotNull(response);
        assertEquals("回答内容", response.answer());
    }

    @Test
    void answerQuestion_throwsWhenNoText() {
        when(documentService.readText("file123")).thenReturn("");

        assertThrows(IllegalArgumentException.class,
                () -> documentAiService.answerQuestion("file123", "问题", Collections.emptyList()));
    }
}
