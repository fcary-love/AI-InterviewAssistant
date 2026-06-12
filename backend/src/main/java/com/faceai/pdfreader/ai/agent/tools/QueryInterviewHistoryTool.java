package com.faceai.pdfreader.ai.agent.tools;

import com.faceai.pdfreader.ai.agent.AgentTool;
import com.faceai.pdfreader.repository.InterviewRepository;
import com.faceai.pdfreader.repository.InterviewQuestionRepository;
import com.faceai.pdfreader.model.InterviewReportSummaryResponse;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class QueryInterviewHistoryTool implements AgentTool {

    private final InterviewRepository interviewRepository;
    private final InterviewQuestionRepository questionRepository;

    public QueryInterviewHistoryTool(InterviewRepository interviewRepository, InterviewQuestionRepository questionRepository) {
        this.interviewRepository = interviewRepository;
        this.questionRepository = questionRepository;
    }

    @Override
    public String name() {
        return "query_interview_history";
    }

    @Override
    public String description() {
        return "查询用户的面试历史记录，包括最近的面试场次、分数和摘要。用于了解用户的面试表现趋势。";
    }

    @Override
    public Map<String, Object> jsonSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "limit", Map.of("type", "integer", "description", "返回的面试场次数，默认5")
                )
        );
    }

    @Override
    public String execute(Map<String, Object> args, Long userId) {
        int limit = 5;
        if (args.containsKey("limit")) {
            limit = ((Number) args.get("limit")).intValue();
        }
        limit = Math.max(1, Math.min(limit, 20));

        List<InterviewReportSummaryResponse> reports = interviewRepository.findReportSummaries(userId);
        if (reports.isEmpty()) {
            return "该用户还没有面试记录。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("最近面试记录（共").append(reports.size()).append("场）：\n");
        int count = 0;
        for (InterviewReportSummaryResponse report : reports) {
            if (count >= limit) break;
            sb.append(count + 1).append(". ");
            sb.append("分数: ").append(report.totalScore() != null ? report.totalScore() : "未评分");
            sb.append(" | ").append(report.summary());
            sb.append(" | 时间: ").append(report.updatedAt());
            sb.append("\n");
            count++;
        }
        return sb.toString();
    }
}
