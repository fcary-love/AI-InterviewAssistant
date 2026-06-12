package com.faceai.pdfreader.service;

import com.faceai.pdfreader.ai.client.AiClient;
import com.faceai.pdfreader.model.InterviewReportSummaryResponse;
import com.faceai.pdfreader.model.InterviewTurnRecord;
import com.faceai.pdfreader.repository.InterviewRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 面试回放增强服务
 * <p>
 * 功能：
 * 1. AI关键词标注 — 提取每题考察的核心关键词，检查用户回答是否命中
 * 2. 时间分布分析 — 统计每题用时，标记异常
 * 3. 面试对比 — 对比两次面试的各维度分数变化
 */
@Service
public class InterviewReplayService {

    private static final Pattern JSON_PATTERN = Pattern.compile("\\[[\\s\\S]*\\]|\\{[\\s\\S]*\\}");
    private static final int TIME_TOO_FAST = 15;
    private static final int TIME_TOO_SLOW = 180;

    private final InterviewRepository interviewRepository;
    private final JdbcTemplate jdbc;
    private final AiClient aiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public InterviewReplayService(InterviewRepository interviewRepository,
                                   JdbcTemplate jdbc,
                                   AiClient aiClient) {
        this.interviewRepository = interviewRepository;
        this.jdbc = jdbc;
        this.aiClient = aiClient;
    }

    /**
     * AI标注关键词命中
     * 对每道题，用AI提取考察的核心关键词，检查用户回答中是否包含
     */
    public List<KeywordAnnotation> annotateKeywords(String sessionId) {
        List<InterviewTurnRecord> turns = interviewRepository.findTurnsBySessionId(sessionId);
        List<KeywordAnnotation> allAnnotations = new ArrayList<>();

        for (InterviewTurnRecord turn : turns) {
            try {
                List<KeywordAnnotation> annotations = extractKeywords(turn.question(), turn.answer());
                // 保存到数据库
                for (KeywordAnnotation ann : annotations) {
                    jdbc.update("""
                            INSERT INTO turn_keyword_annotations (session_id, question_no, keyword, hit, importance, context_snippet)
                            VALUES (?, ?, ?, ?, ?, ?)
                            ON DUPLICATE KEY UPDATE hit = VALUES(hit), context_snippet = VALUES(context_snippet)
                            """,
                            sessionId, turn.questionNo(), ann.keyword(), ann.hit(),
                            ann.importance(), ann.contextSnippet()
                    );
                }
                allAnnotations.addAll(annotations);
            } catch (Exception e) {
                // 单题标注失败不影响整体
            }
        }
        return allAnnotations;
    }

    /**
     * 获取已标注的关键词
     */
    public List<KeywordAnnotation> getAnnotations(String sessionId) {
        return jdbc.queryForList(
                "SELECT * FROM turn_keyword_annotations WHERE session_id = ? ORDER BY question_no, importance",
                sessionId
        ).stream().map(row -> new KeywordAnnotation(
                (String) row.get("keyword"),
                (Boolean) row.get("hit"),
                (String) row.get("importance"),
                (String) row.get("context_snippet"),
                ((Number) row.get("question_no")).intValue()
        )).toList();
    }

    /**
     * 时间分布分析
     */
    public TimeAnalysis analyzeTimeDistribution(String sessionId) {
        List<InterviewTurnRecord> turns = interviewRepository.findTurnsBySessionId(sessionId);
        if (turns.isEmpty()) {
            return new TimeAnalysis(0, 0, 0, 0, List.of());
        }

        List<Integer> durations = turns.stream()
                .map(t -> t.durationSeconds() == null ? 0 : t.durationSeconds())
                .toList();

        int total = durations.stream().mapToInt(Integer::intValue).sum();
        int avg = Math.round((float) total / durations.size());
        int min = durations.stream().mapToInt(Integer::intValue).min().orElse(0);
        int max = durations.stream().mapToInt(Integer::intValue).max().orElse(0);

        List<TimeAnomaly> anomalies = new ArrayList<>();
        for (int i = 0; i < turns.size(); i++) {
            int duration = durations.get(i);
            String anomaly = null;
            if (duration < TIME_TOO_FAST) {
                anomaly = "过快";
            } else if (duration > TIME_TOO_SLOW) {
                anomaly = "过慢";
            }
            if (anomaly != null) {
                anomalies.add(new TimeAnomaly(
                        turns.get(i).questionNo(),
                        turns.get(i).question(),
                        duration,
                        anomaly
                ));
            }
        }

        return new TimeAnalysis(total, avg, min, max, anomalies);
    }

    /**
     * 对比两次面试
     */
    public InterviewComparison compareInterviews(String sessionId1, String sessionId2) {
        var report1 = interviewRepository.findReportBySessionId(null, sessionId1);
        var report2 = interviewRepository.findReportBySessionId(null, sessionId2);

        if (report1.isEmpty() || report2.isEmpty()) {
            throw new IllegalArgumentException("面试报告不存在");
        }

        var r1 = report1.get();
        var r2 = report2.get();

        // 解析分项分数
        Map<String, Integer> scores1 = parseScores(r1.scores());
        Map<String, Integer> scores2 = parseScores(r2.scores());

        // 计算变化
        Map<String, Integer> scoreChanges = new LinkedHashMap<>();
        if (scores1 != null && scores2 != null) {
            for (String key : scores2.keySet()) {
                int oldVal = scores1.getOrDefault(key, 0);
                int newVal = scores2.getOrDefault(key, 0);
                scoreChanges.put(key, newVal - oldVal);
            }
        }

        // 生成AI分析
        String analysis = generateComparisonAnalysis(r1, r2, scoreChanges);

        return new InterviewComparison(
                sessionId1, sessionId2,
                r1.totalScore(), r2.totalScore(),
                r2.totalScore() - r1.totalScore(),
                scoreChanges,
                analysis
        );
    }

    /**
     * 用AI提取关键词并检查命中
     */
    private List<KeywordAnnotation> extractKeywords(String question, String answer) {
        String prompt = """
                请从以下面试问题中提取3-5个考察的核心关键词，并检查候选人的回答是否包含这些关键词。

                请严格按以下JSON格式输出，不要输出其他内容：
                [{"keyword":"关键词","hit":true/false,"importance":"critical/normal/minor","context_snippet":"命中时的上下文片段"}]

                重要性说明：
                - critical：核心概念，必须提到
                - normal：重要内容
                - minor：加分项

                面试问题：%s
                候选人回答：%s
                """.formatted(question, truncate(answer, 500));

        try {
            String raw = aiClient.chat(prompt);
            Matcher m = JSON_PATTERN.matcher(raw);
            if (m.find()) {
                List<Map<String, Object>> parsed = objectMapper.readValue(m.group(), new TypeReference<>() {});
                List<KeywordAnnotation> result = new ArrayList<>();
                for (Map<String, Object> item : parsed) {
                    result.add(new KeywordAnnotation(
                            (String) item.get("keyword"),
                            Boolean.TRUE.equals(item.get("hit")),
                            (String) item.get("importance"),
                            (String) item.get("context_snippet"),
                            0
                    ));
                }
                return result;
            }
        } catch (Exception e) {
            // fallback
        }
        return List.of();
    }

    /**
     * 生成对比分析
     */
    private String generateComparisonAnalysis(Object r1, Object r2, Map<String, Integer> changes) {
        try {
            StringBuilder changeDesc = new StringBuilder();
            changes.forEach((key, value) -> {
                String label = switch (key) {
                    case "technical" -> "技术深度";
                    case "expression" -> "表达逻辑";
                    case "match" -> "岗位匹配";
                    case "problem_solving" -> "问题解决";
                    case "follow_up" -> "追问表现";
                    case "stress_resistance" -> "抗压能力";
                    default -> key;
                };
                changeDesc.append(label).append(": ").append(value > 0 ? "+" : "").append(value).append("分\n");
            });

            String prompt = """
                    请对比两次面试的表现变化，给出简短的中文分析（100字以内）。

                    分项分数变化：
                    %s

                    请分析进步和退步的方面，以及整体趋势。
                    """.formatted(changeDesc);
            return aiClient.chat(prompt);
        } catch (Exception e) {
            return "对比分析生成失败";
        }
    }

    private Map<String, Integer> parseScores(String scoresJson) {
        if (!StringUtils.hasText(scoresJson)) return null;
        try {
            return objectMapper.readValue(scoresJson, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen);
    }

    // ===== 数据记录 =====

    public record KeywordAnnotation(
            String keyword,
            boolean hit,
            String importance,
            String contextSnippet,
            int questionNo
    ) {}

    public record TimeAnalysis(
            int totalSeconds,
            int avgSeconds,
            int minSeconds,
            int maxSeconds,
            List<TimeAnomaly> anomalies
    ) {}

    public record TimeAnomaly(
            int questionNo,
            String question,
            int durationSeconds,
            String anomalyType
    ) {}

    public record InterviewComparison(
            String sessionId1,
            String sessionId2,
            int score1,
            int score2,
            int scoreChange,
            Map<String, Integer> dimensionChanges,
            String analysis
    ) {}
}
