package com.faceai.pdfreader.rag.service;

import com.faceai.pdfreader.repository.InterviewQuestionRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 面试题库 RAG 服务
 * <p>
 * 功能：
 * 1. 将题库向量化存入 Redis
 * 2. 出题时检索相似题目，避免重复
 * 3. 评分时检索参考答案，提高评分准确性
 */
@Service
public class InterviewRagService {

    private final InterviewQuestionRepository questionRepository;
    private final ObjectProvider<VectorStore> interviewVectorStoreProvider;

    public InterviewRagService(
            InterviewQuestionRepository questionRepository,
            @Qualifier("interviewVectorStore") ObjectProvider<VectorStore> interviewVectorStoreProvider
    ) {
        this.questionRepository = questionRepository;
        this.interviewVectorStoreProvider = interviewVectorStoreProvider;
    }

    /**
     * 索引题库到向量库
     *
     * @return 索引的题目数量
     */
    public int indexQuestionBank() {
        VectorStore vectorStore = vectorStore();
        // 清除旧索引
        clearIndex(vectorStore);

        var questions = questionRepository.findAll();
        if (questions.isEmpty()) return 0;

        List<Document> documents = new ArrayList<>();
        for (var q : questions) {
            String text = q.questionText();
            if (!StringUtils.hasText(text)) continue;

            // 拼接问题+参考答案作为向量化文本
            String referenceAnswer = q.referenceAnswer();
            String fullText = StringUtils.hasText(referenceAnswer) ?
                    text + "\n参考答案：" + referenceAnswer : text;

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("questionId", String.valueOf(q.id()));
            metadata.put("category", q.category() != null ? q.category() : "");
            metadata.put("direction", q.direction() != null ? q.direction() : "");
            metadata.put("difficulty", q.difficulty() != null ? q.difficulty() : "");

            documents.add(new Document(fullText, metadata));
        }

        if (!documents.isEmpty()) {
            vectorStore.add(documents);
        }
        return documents.size();
    }

    /**
     * 检索相似题目（用于出题时避免重复）
     *
     * @param query             查询文本（当前面试方向+风格）
     * @param direction         方向过滤
     * @param excludeQuestionIds 排除的题目ID
     * @param topK              返回数量
     * @return 相似题目的文本列表
     */
    public List<String> searchSimilarQuestions(String query, String direction,
                                                List<String> excludeQuestionIds, int topK) {
        try {
            VectorStore vectorStore = vectorStore();
            SearchRequest.Builder builder = SearchRequest.builder()
                    .query(query)
                    .topK(topK + (excludeQuestionIds != null ? excludeQuestionIds.size() : 0));

            // 方向过滤
            if (StringUtils.hasText(direction)) {
                builder.filterExpression("direction == '%s'".formatted(direction));
            }

            List<Document> results = vectorStore.similaritySearch(builder.build());
            if (results == null) return List.of();

            return results.stream()
                    .filter(doc -> {
                        Object qId = doc.getMetadata().get("questionId");
                        String qIdStr = qId != null ? qId.toString() : null;
                        return excludeQuestionIds == null || !excludeQuestionIds.contains(qIdStr);
                    })
                    .limit(topK)
                    .map(doc -> {
                        String text = doc.getText();
                        // 只取问题部分（去掉参考答案）
                        int idx = text.indexOf("\n参考答案：");
                        return idx > 0 ? text.substring(0, idx) : text;
                    })
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * 检索参考答案（用于评分时提高准确性）
     *
     * @param question 问题文本
     * @param topK     返回数量
     * @return 参考答案列表
     */
    public List<String> searchReferenceAnswers(String question, int topK) {
        try {
            VectorStore vectorStore = vectorStore();
            List<Document> results = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(question)
                            .topK(topK)
                            .build()
            );
            if (results == null) return List.of();

            return results.stream()
                    .map(doc -> {
                        String text = doc.getText();
                        int idx = text.indexOf("\n参考答案：");
                        return idx > 0 ? text.substring(idx + 6) : "";
                    })
                    .filter(StringUtils::hasText)
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * 获取索引状态
     */
    public Map<String, Object> getIndexStatus() {
        try {
            VectorStore vectorStore = vectorStore();
            List<Document> sample = vectorStore.similaritySearch(
                    SearchRequest.builder().query("*").topK(1).build()
            );
            boolean available = sample != null;
            int totalQuestions = questionRepository.findAll().size();
            return Map.of(
                    "available", available,
                    "totalQuestions", totalQuestions,
                    "indexed", available && !sample.isEmpty()
            );
        } catch (Exception e) {
            return Map.of("available", false, "totalQuestions", 0, "indexed", false);
        }
    }

    private VectorStore vectorStore() {
        try {
            return interviewVectorStoreProvider.getObject();
        } catch (Exception ex) {
            throw new IllegalArgumentException("Redis 向量库不可用，请先启动 Redis Stack");
        }
    }

    private void clearIndex(VectorStore vectorStore) {
        try {
            List<Document> existing = vectorStore.similaritySearch(
                    SearchRequest.builder().query("*").topK(10000).build()
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
            // Index may not exist yet
        }
    }
}
