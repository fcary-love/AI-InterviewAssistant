package com.faceai.pdfreader.ai.agent.tools;

import com.faceai.pdfreader.ai.agent.AgentTool;
import com.faceai.pdfreader.model.QuestionBankItemResponse;
import com.faceai.pdfreader.repository.InterviewQuestionRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class QueryQuestionBankTool implements AgentTool {

    private final InterviewQuestionRepository questionRepository;

    public QueryQuestionBankTool(InterviewQuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @Override
    public String name() {
        return "query_question_bank";
    }

    @Override
    public String description() {
        return "从题库中搜索面试题目。可按类别、难度、方向筛选。用于为用户推荐练习题目。";
    }

    @Override
    public Map<String, Object> jsonSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "category", Map.of("type", "string", "description", "题目类别，如 'Java 基础', 'Vue3', 'MySQL' 等"),
                        "difficulty", Map.of("type", "string", "description", "难度: '简单', '标准', '困难'"),
                        "direction", Map.of("type", "string", "description", "方向: '后端开发', '前端开发'"),
                        "limit", Map.of("type", "integer", "description", "返回题目数量，默认5")
                )
        );
    }

    @Override
    public String execute(Map<String, Object> args, Long userId) {
        String category = args.containsKey("category") ? (String) args.get("category") : null;
        String difficulty = args.containsKey("difficulty") ? (String) args.get("difficulty") : null;
        String direction = args.containsKey("direction") ? (String) args.get("direction") : null;
        int limit = args.containsKey("limit") ? ((Number) args.get("limit")).intValue() : 5;

        List<QuestionBankItemResponse> questions = questionRepository.listQuestions(direction, category, difficulty, null, limit);
        if (questions.isEmpty()) {
            return "未找到匹配的题目。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("找到 ").append(questions.size()).append(" 道题目：\n\n");
        for (int i = 0; i < questions.size(); i++) {
            QuestionBankItemResponse q = questions.get(i);
            sb.append(i + 1).append(". [").append(q.category()).append("][").append(q.difficulty()).append("] ");
            sb.append(q.questionText()).append("\n");
            if (q.referenceAnswer() != null && !q.referenceAnswer().isBlank()) {
                String preview = q.referenceAnswer().length() > 200
                        ? q.referenceAnswer().substring(0, 200) + "..."
                        : q.referenceAnswer();
                sb.append("   参考答案: ").append(preview).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
