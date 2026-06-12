package com.faceai.pdfreader.ai.agent.tools;

import com.faceai.pdfreader.ai.agent.AgentTool;
import com.faceai.pdfreader.model.InterviewTurnRecord;
import com.faceai.pdfreader.repository.InterviewRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Component
public class AnalyzeInterviewDetailTool implements AgentTool {

    private final InterviewRepository interviewRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AnalyzeInterviewDetailTool(InterviewRepository interviewRepository) {
        this.interviewRepository = interviewRepository;
    }

    @Override
    public String name() {
        return "analyze_interview_detail";
    }

    @Override
    public String description() {
        return "详细分析某次面试的逐题表现。传入sessionId，返回每道题的问题、回答摘要、评分和点评。用于深入了解某次面试的具体表现。";
    }

    @Override
    public Map<String, Object> jsonSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "sessionId", Map.of("type", "string", "description", "面试会话ID")
                ),
                "required", List.of("sessionId")
        );
    }

    @Override
    public String execute(Map<String, Object> args, Long userId) {
        String sessionId = (String) args.get("sessionId");
        if (!StringUtils.hasText(sessionId)) {
            return "请提供sessionId。";
        }

        List<InterviewTurnRecord> turns = interviewRepository.findTurnsBySessionId(sessionId);
        if (turns.isEmpty()) {
            return "未找到该面试的回答记录。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("面试详细分析（").append(sessionId).append("）：\n\n");

        int totalScore = 0;
        int scoredCount = 0;

        for (InterviewTurnRecord turn : turns) {
            sb.append("第").append(turn.questionNo()).append("题：");
            sb.append(turn.question()).append("\n");
            sb.append("  回答摘要：").append(truncate(turn.answer(), 100)).append("\n");
            sb.append("  用时：").append(turn.durationSeconds()).append("秒\n");

            if (turn.score() != null) {
                sb.append("  评分：").append(turn.score()).append("分");
                totalScore += turn.score();
                scoredCount++;

                // 分项分数
                Map<String, Integer> scores = parseScores(turn.scores());
                if (scores != null) {
                    sb.append(" (");
                    sb.append("技术:").append(scores.getOrDefault("technical", 0));
                    sb.append(" 表达:").append(scores.getOrDefault("expression", 0));
                    sb.append(" 匹配:").append(scores.getOrDefault("match", 0));
                    sb.append(")");
                }
                sb.append("\n");
            }

            if (StringUtils.hasText(turn.aiComment())) {
                sb.append("  点评：").append(turn.aiComment()).append("\n");
            }

            if (Boolean.TRUE.equals(turn.followUp())) {
                sb.append("  [追问题]\n");
            }
            sb.append("\n");
        }

        if (scoredCount > 0) {
            int avg = Math.round((float) totalScore / scoredCount);
            sb.append("平均分：").append(avg).append("分\n");
        }

        return sb.toString();
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
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
