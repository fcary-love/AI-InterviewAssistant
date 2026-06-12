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
public class InterviewRagConfig {

    @Bean
    @Lazy
    public VectorStore interviewVectorStore(
            JedisPooled jedisPooled,
            EmbeddingModel embeddingModel,
            @Value("${app.interview-rag.initialize-schema:true}") boolean initializeSchema
    ) {
        return RedisVectorStore.builder(jedisPooled, embeddingModel)
                .indexName("interview_questions_idx")
                .prefix("interviewq")
                .metadataFields(List.of(
                        MetadataField.tag("questionId"),
                        MetadataField.tag("category"),
                        MetadataField.tag("direction"),
                        MetadataField.tag("difficulty")
                ))
                .initializeSchema(initializeSchema)
                .build();
    }
}
