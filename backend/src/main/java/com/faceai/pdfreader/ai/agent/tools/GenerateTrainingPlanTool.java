package com.faceai.pdfreader.ai.agent.tools;

import com.faceai.pdfreader.ai.agent.AgentTool;
import com.faceai.pdfreader.service.KnowledgeGraphService;
import com.faceai.pdfreader.service.KnowledgeGraphService.StudySuggestion;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GenerateTrainingPlanTool implements AgentTool {

    private final KnowledgeGraphService knowledgeGraphService;

    public GenerateTrainingPlanTool(KnowledgeGraphService knowledgeGraphService) {
        this.knowledgeGraphService = knowledgeGraphService;
    }

    @Override
    public String name() {
        return "generate_training_plan";
    }

    @Override
    public String description() {
        return "基于用户的技能树掌握度，生成个性化训练计划。分析薄弱知识点，推荐优先学习内容。";
    }

    @Override
    public Map<String, Object> jsonSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "direction", Map.of("type", "string", "description", "方向，如：后端开发")
                )
        );
    }

    @Override
    public String execute(Map<String, Object> args, Long userId) {
        String direction = args.containsKey("direction") ? (String) args.get("direction") : "后端开发";

        List<StudySuggestion> suggestions = knowledgeGraphService.suggestNextStudy(userId, direction);
        if (suggestions.isEmpty()) {
            return "暂无学习建议。请先完成一些面试练习，系统会根据你的表现生成个性化建议。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("个性化训练计划（").append(direction).append("）：\n\n");
        sb.append("根据你的面试表现，建议优先学习以下内容：\n\n");

        int index = 1;
        for (StudySuggestion suggestion : suggestions) {
            sb.append(index++).append(". ").append(suggestion.name());
            sb.append(" [").append(suggestion.category()).append("]");
            sb.append(" 难度：").append(suggestion.difficulty());
            sb.append("\n   当前掌握度：").append(String.format("%.0f", suggestion.currentMastery())).append("%");
            sb.append("\n   原因：").append(suggestion.reason());
            sb.append("\n\n");
        }

        sb.append("建议每天练习1-2个知识点，通过模拟面试来检验学习效果。");

        return sb.toString();
    }
}
