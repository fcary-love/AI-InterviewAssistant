package com.faceai.pdfreader.rag.service;

import com.faceai.pdfreader.config.RagProperties;
import com.faceai.pdfreader.model.PageText;
import com.faceai.pdfreader.rag.model.ChunkedText;
import com.faceai.pdfreader.rag.splitter.TextChunker;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class KnowledgeRagIndexService {

    private final RagProperties ragProperties;
    private final TextChunker textChunker;
    private final ObjectProvider<VectorStore> knowledgeVectorStoreProvider;

    public KnowledgeRagIndexService(
            RagProperties ragProperties,
            TextChunker textChunker,
            @Qualifier("knowledgeVectorStore") ObjectProvider<VectorStore> knowledgeVectorStoreProvider
    ) {
        this.ragProperties = ragProperties;
        this.textChunker = textChunker;
        this.knowledgeVectorStoreProvider = knowledgeVectorStoreProvider;
    }

    public int indexText(String userId, String fileId, String fileName, String docType, String text) {
        if (!StringUtils.hasText(text)) {
            return 0;
        }
        VectorStore vectorStore = vectorStore();
        deleteExisting(vectorStore, fileId, docType, userId);

        List<PageText> pageTexts = List.of(new PageText(1, text));
        List<ChunkedText> chunks = textChunker.split(fileId, fileName, pageTexts);
        if (chunks.isEmpty()) {
            return 0;
        }

        List<Document> documents = new ArrayList<>();
        for (ChunkedText chunk : chunks) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("fileId", chunk.metadata().fileId());
            metadata.put("fileName", chunk.metadata().fileName());
            metadata.put("docType", docType);
            metadata.put("userId", String.valueOf(userId));
            metadata.put("pageNumber", chunk.metadata().pageNumber());
            metadata.put("chunkIndex", chunk.metadata().chunkIndex());
            documents.add(new Document(chunk.text(), metadata));
        }

        vectorStore.add(documents);
        return documents.size();
    }

    private VectorStore vectorStore() {
        try {
            return knowledgeVectorStoreProvider.getObject();
        } catch (Exception ex) {
            throw new IllegalArgumentException("Redis 向量库不可用，请先启动 Redis Stack 后再使用知识库索引");
        }
    }

    private void deleteExisting(VectorStore vectorStore, String fileId, String docType, String userId) {
        try {
            String filter = "fileId == '%s' && docType == '%s' && userId == '%s'"
                    .formatted(fileId, docType, userId);
            List<Document> existing = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query("*")
                            .topK(1000)
                            .filterExpression(filter)
                            .build()
            );
            if (existing != null && !existing.isEmpty()) {
                List<String> ids = existing.stream()
                        .map(Document::getId)
                        .filter(StringUtils::hasText)
                        .toList();
                if (!ids.isEmpty()) {
                    vectorStore.delete(ids);
                }
            }
        } catch (Exception ignored) {
            // filter may not match anything, that's fine
        }
    }
}
