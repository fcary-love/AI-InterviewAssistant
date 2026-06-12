package com.faceai.pdfreader.ai.agent.tools;

import com.faceai.pdfreader.ai.agent.AgentTool;
import com.faceai.pdfreader.service.KnowledgeGraphService;
import com.faceai.pdfreader.service.KnowledgeGraphService.SkillTreeNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class QuerySkillTreeTool implements AgentTool {

    private final KnowledgeGraphService knowledgeGraphService;

    public QuerySkillTreeTool(KnowledgeGraphService knowledgeGraphService) {
        this.knowledgeGraphService = knowledgeGraphService;
    }

    @Override
    public String name() {
        return "query_skill_tree";
    }

    @Override
    public String description() {
        return "查询用户的技能树和知识点掌握度。返回各知识点的掌握程度，用于分析用户的能力分布和薄弱点。";
    }

    @Override
    public Map<String, Object> jsonSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "direction", Map.of("type", "string", "description", "方向，如：后端开发、前端开发")
                )
        );
    }

    @Override
    public String execute(Map<String, Object> args, Long userId) {
        String direction = args.containsKey("direction") ? (String) args.get("direction") : "后端开发";

        List<SkillTreeNode> tree = knowledgeGraphService.getUserSkillTree(userId, direction);
        if (tree.isEmpty()) {
            return "该方向暂无知识点数据。";
        }

        // 按掌握度分组
        Map<String, List<SkillTreeNode>> byCategory = tree.stream()
                .collect(Collectors.groupingBy(SkillTreeNode::category));

        StringBuilder sb = new StringBuilder();
        sb.append("技能树（").append(direction).append("）：\n\n");

        for (var entry : byCategory.entrySet()) {
            sb.append("【").append(entry.getKey()).append("】\n");
            for (SkillTreeNode node : entry.getValue()) {
                String status = node.mastery() >= 70 ? "✓已掌握" :
                        node.mastery() >= 40 ? "△学习中" : "✗待加强";
                sb.append("  ").append(status).append(" ").append(node.name());
                sb.append(" (掌握度: ").append(String.format("%.0f", node.mastery())).append("%)");
                if (node.attemptCount() > 0) {
                    sb.append(" [练习").append(node.attemptCount()).append("次]");
                }
                sb.append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}
