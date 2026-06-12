package com.faceai.pdfreader.ai.agent.tools;

import com.faceai.pdfreader.ai.agent.AgentTool;
import com.faceai.pdfreader.repository.InterviewRepository;
import com.faceai.pdfreader.repository.TrainingPlanRepository;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class QueryWeakPointsTool implements AgentTool {

    private final TrainingPlanRepository trainingPlanRepository;
    private final JdbcTemplate jdbcTemplate;

    public QueryWeakPointsTool(TrainingPlanRepository trainingPlanRepository, JdbcTemplate jdbcTemplate) {
        this.trainingPlanRepository = trainingPlanRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String name() {
        return "query_weak_points";
    }

    @Override
    public String description() {
        return "分析用户的薄弱知识点。返回用户在面试中得分较低的题目类别、平均分数和具体薄弱领域。用于制定针对性训练计划。";
    }

    @Override
    public Map<String, Object> jsonSchema() {
        return Map.of("type", "object", "properties", Map.of());
    }

    @Override
    public String execute(Map<String, Object> args, Long userId) {
        List<String> weakCategories = trainingPlanRepository.weakCategories(userId, 10);
        if (weakCategories.isEmpty()) {
            return "该用户暂无面试数据，无法分析薄弱点。建议先完成几场模拟面试。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("薄弱知识点分析：\n\n");

        sb.append("薄弱类别（按薄弱程度排序）：\n");
        for (int i = 0; i < weakCategories.size(); i++) {
            sb.append(i + 1).append(". ").append(weakCategories.get(i)).append("\n");
        }

        sb.append("\n各类别详细数据：\n");
        for (String category : weakCategories) {
            List<Map<String, Object>> details = jdbcTemplate.queryForList("""
                    SELECT
                        COUNT(*) AS total,
                        ROUND(AVG(t.score)) AS avg_score,
                        MIN(t.score) AS min_score
                    FROM interview_turns t
                    INNER JOIN interview_sessions s ON s.session_id = t.session_id
                    LEFT JOIN interview_questions q ON q.question_text = t.question
                    WHERE s.user_id = ? AND COALESCE(q.category, '综合复盘') = ? AND t.score IS NOT NULL
                    """, userId, category);
            if (!details.isEmpty()) {
                Map<String, Object> d = details.get(0);
                sb.append("- ").append(category).append(": ");
                sb.append("共").append(d.get("total")).append("题, ");
                sb.append("平均").append(d.get("avg_score")).append("分, ");
                sb.append("最低").append(d.get("min_score")).append("分\n");
            }
        }

        return sb.toString();
    }
}
