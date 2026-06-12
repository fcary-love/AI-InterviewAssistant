package com.faceai.pdfreader.rag.skills;

import com.faceai.pdfreader.config.RagProperties;
import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class KnowledgeRagQaSkill {

    private final RagProperties ragProperties;
    private final ObjectProvider<VectorStore> knowledgeVectorStoreProvider;

    public KnowledgeRagQaSkill(
            RagProperties ragProperties,
            @Qualifier("knowledgeVectorStore") ObjectProvider<VectorStore> knowledgeVectorStoreProvider
    ) {
        this.ragProperties = ragProperties;
        this.knowledgeVectorStoreProvider = knowledgeVectorStoreProvider;
    }

    public List<Document> search(String userId, String query, String docTypeFilter, int topK) {
        String filter = buildFilter(userId, docTypeFilter);
        return vectorStore().similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(topK > 0 ? topK : ragProperties.topK())
                        .similarityThreshold(ragProperties.similarityThreshold())
                        .filterExpression(filter)
                        .build()
        );
    }

    public List<Document> search(String userId, String query, String docTypeFilter) {
        return search(userId, query, docTypeFilter, ragProperties.topK());
    }

    private VectorStore vectorStore() {
        try {
            return knowledgeVectorStoreProvider.getObject();
        } catch (Exception ex) {
            throw new IllegalArgumentException("Redis 向量库不可用，请先启动 Redis Stack 后再使用知识库检索");
        }
    }

    private String buildFilter(String userId, String docTypeFilter) {
        String base = "userId == '%s'".formatted(userId);
        if (StringUtils.hasText(docTypeFilter)) {
            return base + " && docType == '%s'".formatted(docTypeFilter);
        }
        return base;
    }
}
