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
public class RagConfig {

    @Bean
    public JedisPooled jedisPooled(
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port
    ) {
        return new JedisPooled(host, port);
    }

    @Bean
    @Lazy
    public VectorStore vectorStore(
            JedisPooled jedisPooled,
            EmbeddingModel embeddingModel,
            @Value("${spring.ai.vectorstore.redis.index-name:pdf_chunks_idx}") String indexName,
            @Value("${spring.ai.vectorstore.redis.prefix:pdfchunk}") String prefix,
            @Value("${spring.ai.vectorstore.redis.initialize-schema:true}") boolean initializeSchema
    ) {
        return RedisVectorStore.builder(jedisPooled, embeddingModel)
                .indexName(indexName)
                .prefix(prefix)
                .metadataFields(List.of(
                        MetadataField.tag("fileId"),
                        MetadataField.tag("fileName"),
                        MetadataField.numeric("pageNumber"),
                        MetadataField.numeric("chunkIndex")
                ))
                .initializeSchema(initializeSchema)
                .build();
    }
}
