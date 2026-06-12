package com.faceai.pdfreader.config;

import java.util.List;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore.MetadataField;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import redis.clients.jedis.JedisPooled;

@Configuration
public class KnowledgeRagConfig {

    @Bean
    @Lazy
    public VectorStore knowledgeVectorStore(
            JedisPooled jedisPooled,
            EmbeddingModel embeddingModel,
            @Value("${app.knowledge-rag.initialize-schema:true}") boolean initializeSchema
    ) {
        return RedisVectorStore.builder(jedisPooled, embeddingModel)
                .indexName("knowledge_chunks_idx")
                .prefix("knowledgechunk")
                .metadataFields(List.of(
                        MetadataField.tag("fileId"),
                        MetadataField.tag("fileName"),
                        MetadataField.tag("docType"),
                        MetadataField.tag("userId"),
                        MetadataField.numeric("pageNumber"),
                        MetadataField.numeric("chunkIndex")
                ))
                .initializeSchema(initializeSchema)
                .build();
    }
}
