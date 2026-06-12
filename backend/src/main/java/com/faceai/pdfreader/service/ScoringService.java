package com.faceai.pdfreader.service;

import com.faceai.pdfreader.ai.client.AiClient;
import com.faceai.pdfreader.repository.InterviewRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 带置信度的评分服务
 * <p>
 * 核心思想：AI 评分存在随机性。通过多次评分 + 置信度计算，
 * 在评分结果不确定时自动重评，提高评分的稳定性和准确性。
 */
@Service
public class ScoringService {

    private static final int MAX_ATTEMPTS = 3;
    private static final double CONFIDENCE_THRESHOLD = 70.0;
    private static final Pattern JSON_PATTERN = Pattern.compile("\\{[\\s\\S]*}");
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final AiClient aiClient;

    public ScoringService(AiClient aiClient) {
        this.aiClient = aiClient;
    }

    /**
     * 带置信度的评分
     *
     * @param question  面试问题
     * @param answer    候选人回答
     * @param direction 方向
     * @return EvaluationResult 包含分数、评论、置信度
     */
    public EvaluationWithConfidence evaluateWithConfidence(String question, String answer, String direction) {
        List<Map<String, Integer>> allScores = new ArrayList<>();
        List<String> allComments = new ArrayList<>();
        List<Double> confidences = new ArrayList<>();

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                String prompt = buildEvaluationPrompt(question, answer, direction);
                String raw = aiClient.chat(prompt);
                Map<String, Object> parsed = parseEvaluationJson(raw, answer);

                Map<String, Integer> scores = extractScores(parsed, answer);
                String comment = parsed.get("comment") != null ?
                        parsed.get("comment").toString().trim() : "";
                double confidence = calculateConfidence(scores, answer.length());

                allScores.add(scores);
                allComments.add(comment);
                confidences.add(confidence);

                // 如果置信度足够高，直接返回
                if (confidence >= CONFIDENCE_THRESHOLD) {
                    return buildResult(scores, comment, confidence, attempt, allScores);
                }
            } catch (Exception e) {
                // 评分失败，继续尝试
            }
        }

        // 所有尝试完成后，取加权平均
        return aggregateAttempts(allScores, allComments, confidences);
    }

    /**
     * 计算评分置信度
     * <p>
     * 置信度基于三个因素：
     * 1. 各维度分数的标准差（标准差大 → 分数分散 → 置信度低）
     * 2. 回答长度（极短回答 → 信息不足 → 置信度低）
     * 3. 分数分布合理性（极端分数 → 置信度低）
     */
    private double calculateConfidence(Map<String, Integer> scores, int answerLength) {
        if (scores == null || scores.isEmpty()) return 0.0;

        List<Integer> values = new ArrayList<>(scores.values());
        double mean = values.stream().mapToInt(Integer::intValue).average().orElse(50);
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average().orElse(0);
        double stdDev = Math.sqrt(variance);

        // 基础置信度：基于标准差（标准差越小，置信度越高）
        double stdDevScore = Math.max(0, 100 - stdDev * 2);

        // 长度惩罚：极短回答置信度降低
        double lengthFactor = 1.0;
        if (answerLength < 20) {
            lengthFactor = 0.5;
        } else if (answerLength < 50) {
            lengthFactor = 0.7;
        }

        // 极端分数惩罚
        boolean hasExtreme = values.stream().anyMatch(v -> v < 15 || v > 95);
        double extremeFactor = hasExtreme ? 0.8 : 1.0;

        return stdDevScore * lengthFactor * extremeFactor;
    }

    /**
     * 聚合多次评分结果
     * 去掉最高和最低分，取中间值的平均
     */
    private EvaluationWithConfidence aggregateAttempts(
            List<Map<String, Integer>> allScores,
            List<String> allComments,
            List<Double> confidences) {

        if (allScores.isEmpty()) {
            return new EvaluationWithConfidence(50, "评分失败，使用默认分数", 0.0, 0, Map.of(
                    "technical", 50, "expression", 50, "match", 50,
                    "problem_solving", 50, "follow_up", 50, "stress_resistance", 50
            ));
        }

        // 对每个维度，收集所有尝试的分数
        Map<String, List<Integer>> dimensionScores = new LinkedHashMap<>();
        for (Map<String, Integer> scores : allScores) {
            scores.forEach((key, value) ->
                    dimensionScores.computeIfAbsent(key, k -> new ArrayList<>()).add(value)
            );
        }

        // 对每个维度，去掉最高最低取平均
        Map<String, Integer> aggregatedScores = new LinkedHashMap<>();
        for (Map.Entry<String, List<Integer>> entry : dimensionScores.entrySet()) {
            List<Integer> values = entry.getValue();
            if (values.size() == 1) {
                aggregatedScores.put(entry.getKey(), values.get(0));
            } else {
                // 去掉最高和最低
                List<Integer> sorted = new ArrayList<>(values);
                sorted.sort(Integer::compareTo);
                List<Integer> trimmed = sorted.subList(1, sorted.size() - 1);
                double avg = trimmed.stream().mapToInt(Integer::intValue).average().orElse(50);
                aggregatedScores.put(entry.getKey(), (int) Math.round(avg));
            }
        }

        // 计算综合分
        int overall = (int) Math.round(
                aggregatedScores.values().stream().mapToInt(Integer::intValue).average().orElse(50)
        );

        // 取置信度最高的评论
        String bestComment = "";
        double maxConfidence = 0;
        for (int i = 0; i < Math.min(allComments.size(), confidences.size()); i++) {
            if (confidences.get(i) > maxConfidence) {
                maxConfidence = confidences.get(i);
                bestComment = allComments.get(i);
            }
        }

        double avgConfidence = confidences.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        return new EvaluationWithConfidence(overall, bestComment, avgConfidence, allScores.size(), aggregatedScores);
    }

    private EvaluationWithConfidence buildResult(Map<String, Integer> scores, String comment,
                                                  double confidence, int attempts,
                                                  List<Map<String, Integer>> allScores) {
        int overall = (int) Math.round(
                scores.values().stream().mapToInt(Integer::intValue).average().orElse(50)
        );
        return new EvaluationWithConfidence(overall, comment, confidence, attempts, scores);
    }

    private String buildEvaluationPrompt(String question, String answer, String direction) {
        return """
                你是一名严格但公正的技术面试官，正在面试「%s」方向的候选人。
                请对候选人本题回答进行六维度评分。

                请严格按以下 JSON 格式输出，不要输出其他内容：
                {"technical":分数,"expression":分数,"match":分数,"problem_solving":分数,"follow_up":分数,"stress_resistance":分数,"overall":分数,"comment":"点评内容"}

                评分规则（每项 0-100）：
                - technical（技术深度）：考察技术概念准确性、深度、实践经验
                - expression（表达逻辑）：考察回答结构、条理性、表达清晰度
                - match（岗位匹配）：考察回答与目标岗位的关联度
                - problem_solving（问题解决能力）：考察分析思路、方案设计、权衡取舍能力
                - follow_up（追问表现）：考察追问时的补充深度、临场反应、知识延伸
                - stress_resistance（抗压能力）：考察压力下的逻辑清晰度、情绪管理
                - overall（综合分数）：综合以上六维度的评估
                - comment：一句到两句中文点评，说明得分原因和改进建议

                分数参考：
                0-39：基本没有回答问题，或明显跑题
                40-59：回答很浅，缺少关键概念或细节
                60-74：基本回答到点，但深度不足
                75-89：回答较完整，有清晰思路
                90-100：回答深入，有原理、实践和结果

                面试方向：%s
                面试问题：%s
                候选人回答：%s
                """.formatted(direction, direction, question, answer);
    }

    private Map<String, Object> parseEvaluationJson(String raw, String answer) {
        Matcher m = JSON_PATTERN.matcher(raw);
        if (m.find()) {
            try {
                return objectMapper.readValue(m.group(), new TypeReference<>() {});
            } catch (JsonProcessingException e) {
                // fallback
            }
        }
        // fallback: 返回默认值
        int fallback = scoreByLength(answer);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("technical", fallback);
        result.put("expression", fallback);
        result.put("match", fallback);
        result.put("problem_solving", fallback);
        result.put("follow_up", fallback);
        result.put("stress_resistance", fallback);
        result.put("overall", fallback);
        result.put("comment", "");
        return result;
    }

    private Map<String, Integer> extractScores(Map<String, Object> parsed, String answer) {
        int fallback = scoreByLength(answer);
        Map<String, Integer> scores = new LinkedHashMap<>();
        scores.put("technical", toInt(parsed.get("technical"), fallback));
        scores.put("expression", toInt(parsed.get("expression"), fallback));
        scores.put("match", toInt(parsed.get("match"), fallback));
        scores.put("problem_solving", toInt(parsed.get("problem_solving"), fallback));
        scores.put("follow_up", toInt(parsed.get("follow_up"), fallback));
        scores.put("stress_resistance", toInt(parsed.get("stress_resistance"), fallback));
        return scores;
    }

    private int toInt(Object value, int defaultVal) {
        if (value instanceof Number n) return Math.max(0, Math.min(100, n.intValue()));
        if (value instanceof String s) {
            try { return Math.max(0, Math.min(100, Integer.parseInt(s))); }
            catch (NumberFormatException e) { return defaultVal; }
        }
        return defaultVal;
    }

    private int scoreByLength(String answer) {
        int length = answer.trim().length();
        if (length < 20) return 45;
        if (length < 80) return 62;
        if (length < 180) return 74;
        return 82;
    }

    /**
     * 评分结果（包含置信度）
     */
    public record EvaluationWithConfidence(
            int overall,
            String comment,
            double confidence,
            int attempts,
            Map<String, Integer> scores
    ) {}
}
