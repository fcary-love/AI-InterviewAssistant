package com.faceai.pdfreader.rag.skills;

import com.faceai.pdfreader.ai.client.AiClient;
import com.faceai.pdfreader.config.RagProperties;
import com.faceai.pdfreader.model.ChatHistoryMessage;
import com.faceai.pdfreader.model.PdfContentResponse;
import com.faceai.pdfreader.model.RagQaResponse;
import com.faceai.pdfreader.model.RagReferenceChunk;
import com.faceai.pdfreader.service.PdfService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SinglePdfRagQaSkill {

    private final RagProperties ragProperties;
    private final PdfService pdfService;
    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final AiClient aiClient;

    public SinglePdfRagQaSkill(
            RagProperties ragProperties,
            PdfService pdfService,
            ObjectProvider<VectorStore> vectorStoreProvider,
            AiClient aiClient
    ) {
        this.ragProperties = ragProperties;
        this.pdfService = pdfService;
        this.vectorStoreProvider = vectorStoreProvider;
        this.aiClient = aiClient;
    }

    public RagQaResponse execute(String fileId, String question, List<ChatHistoryMessage> history) {
        ensureEnabled();
        PdfContentResponse content = pdfService.getContent(fileId);
        List<Document> documents = vectorStore().similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(ragProperties.topK())
                        .similarityThreshold(ragProperties.similarityThreshold())
                        .filterExpression("fileId == '" + fileId + "'")
                        .build()
        );

        if (documents == null || documents.isEmpty()) {
            throw new IllegalArgumentException("当前 PDF 还没有 RAG 索引，或未检索到相关内容，请先构建索引");
        }

        String referencesText = documents.stream()
                .map(document -> {
                    Object pageNumber = document.getMetadata().get("pageNumber");
                    Object chunkIndex = document.getMetadata().get("chunkIndex");
                    return "参考片段（第 %s 页，第 %s 段）：\n%s".formatted(pageNumber, chunkIndex, document.getText());
                })
                .collect(Collectors.joining("\n\n"));

        String prompt = """
                你是一名基于知识库的 PDF 问答助手。请严格根据下面检索出来的参考片段回答用户问题。
                规则：
                1. 如果参考片段足够，请直接用中文回答
                2. 如果参考片段不足，请明确说“知识库中未检索到足够信息”
                3. 不要编造
                4. 如果合适，可以在回答中提到参考页码

                最近对话：
                %s

                用户问题：
                %s

                检索参考片段：
                %s
                """.formatted(formatHistory(history), question, referencesText);

        String answer = aiClient.chat(prompt);
        List<RagReferenceChunk> references = documents.stream()
                .map(document -> new RagReferenceChunk(
                        document.getText(),
                        toInt(document.getMetadata().get("pageNumber")),
                        toInt(document.getMetadata().get("chunkIndex"))
                ))
                .toList();

        return new RagQaResponse(content.fileId(), content.fileName(), question, answer, references);
    }

    private VectorStore vectorStore() {
        try {
            return vectorStoreProvider.getObject();
        } catch (Exception ex) {
            throw new IllegalArgumentException("Redis 向量库不可用，请先启动 Redis Stack 后再使用 RAG 问答");
        }
    }

    private int toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private void ensureEnabled() {
        if (!ragProperties.enabled()) {
            throw new IllegalArgumentException("RAG 功能未开启，请在 application.yml 中设置 app.rag.enabled=true");
        }
    }

    private String formatHistory(List<ChatHistoryMessage> history) {
        if (history == null || history.isEmpty()) {
            return "暂无历史对话。";
        }
        return history.stream()
                .filter(message -> message != null && StringUtils.hasText(message.content()))
                .limit(8)
                .map(message -> {
                    String role = "user".equalsIgnoreCase(message.role()) ? "用户" : "助手";
                    return role + "：" + limitHistoryText(message.content());
                })
                .collect(Collectors.joining("\n"));
    }

    private String limitHistoryText(String text) {
        String normalized = text.strip();
        if (normalized.length() <= 600) {
            return normalized;
        }
        return normalized.substring(0, 600);
    }
}
