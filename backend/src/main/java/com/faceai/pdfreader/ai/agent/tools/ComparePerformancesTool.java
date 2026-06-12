package com.faceai.pdfreader.ai.agent.tools;

import com.faceai.pdfreader.ai.agent.AgentTool;
import com.faceai.pdfreader.model.InterviewReportSummaryResponse;
import com.faceai.pdfreader.repository.InterviewRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Component
public class ComparePerformancesTool implements AgentTool {

    private final InterviewRepository interviewRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ComparePerformancesTool(InterviewRepository interviewRepository) {
        this.interviewRepository = interviewRepository;
    }

    @Override
    public String name() {
        return "compare_performances";
    }

    @Override
    public String description() {
        return "对比用户最近多次面试的表现趋势。分析各维度分数的变化，识别进步和退步的方面。";
    }

    @Override
    public Map<String, Object> jsonSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "limit", Map.of("type", "integer", "description", "对比的面试场次数，默认5")
                )
        );
    }

    @Override
    public String execute(Map<String, Object> args, Long userId) {
        int limit = 5;
        if (args.containsKey("limit")) {
            limit = ((Number) args.get("limit")).intValue();
        }
        limit = Math.max(2, Math.min(limit, 10));

        List<InterviewReportSummaryResponse> reports = interviewRepository.findReportSummaries(userId);
        if (reports.size() < 2) {
            return "至少需要2场面试记录才能进行对比分析。";
        }

        // 取最近N场
        List<InterviewReportSummaryResponse> recent = reports.subList(0, Math.min(limit, reports.size()));

        StringBuilder sb = new StringBuilder();
        sb.append("面试表现对比分析（最近").append(recent.size()).append("场）：\n\n");

        // 分数趋势
        sb.append("分数趋势：");
        for (int i = recent.size() - 1; i >= 0; i--) {
            var r = recent.get(i);
            sb.append(r.totalScore() != null ? r.totalScore() : "?");
            if (i > 0) sb.append(" → ");
        }
        sb.append("\n\n");

        // 各维度趋势
        Map<String, Integer> firstScores = parseScores(recent.get(recent.size() - 1).scores());
        Map<String, Integer> lastScores = parseScores(recent.get(0).scores());

        if (firstScores != null && lastScores != null) {
            sb.append("维度变化：\n");
            String[] keys = {"technical", "expression", "match", "problem_solving", "follow_up", "stress_resistance"};
            String[] labels = {"技术深度", "表达逻辑", "岗位匹配", "问题解决", "追问表现", "抗压能力"};

            for (int i = 0; i < keys.length; i++) {
                int oldVal = firstScores.getOrDefault(keys[i], 0);
                int newVal = lastScores.getOrDefault(keys[i], 0);
                int change = newVal - oldVal;
                String arrow = change > 0 ? "↑" : change < 0 ? "↓" : "→";
                sb.append("  ").append(labels[i]).append(": ").append(oldVal)
                  .append(" → ").append(newVal).append(" (").append(arrow)
                  .append(Math.abs(change)).append("分)\n");
            }
        }

        // 总结
        sb.append("\n总结：");
        if (recent.size() >= 2) {
            int firstScore = recent.get(recent.size() - 1).totalScore() != null ? recent.get(recent.size() - 1).totalScore() : 0;
            int lastScore = recent.get(0).totalScore() != null ? recent.get(0).totalScore() : 0;
            int totalChange = lastScore - firstScore;
            if (totalChange > 10) {
                sb.append("整体表现有明显进步，继续加油！");
            } else if (totalChange > 0) {
                sb.append("表现稳步提升，保持练习节奏。");
            } else if (totalChange == 0) {
                sb.append("表现稳定，可以尝试更高难度的题目。");
            } else {
                sb.append("近期表现有所下滑，建议回顾薄弱知识点。");
            }
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
}
