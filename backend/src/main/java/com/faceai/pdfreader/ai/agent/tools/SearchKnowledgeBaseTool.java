package com.faceai.pdfreader.ai.agent.tools;

import com.faceai.pdfreader.ai.agent.AgentTool;
import com.faceai.pdfreader.rag.skills.KnowledgeRagQaSkill;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SearchKnowledgeBaseTool implements AgentTool {

    private final KnowledgeRagQaSkill knowledgeRagQaSkill;

    public SearchKnowledgeBaseTool(KnowledgeRagQaSkill knowledgeRagQaSkill) {
        this.knowledgeRagQaSkill = knowledgeRagQaSkill;
    }

    @Override
    public String name() {
        return "search_knowledge_base";
    }

    @Override
    public String description() {
        return "从用户的个人知识库中搜索相关信息。知识库包含用户的简历、JD（岗位描述）、面试题等。当需要了解用户的背景、技能或目标岗位时使用。";
    }

    @Override
    public Map<String, Object> jsonSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "query", Map.of("type", "string", "description", "搜索关键词或问题"),
                        "docType", Map.of("type", "string", "description", "文档类型过滤: 'resume'(简历), 'jd'(岗位描述), 'question'(面试题), 'report'(面试报告)。留空则搜索全部。"),
                        "topK", Map.of("type", "integer", "description", "返回结果数量，默认4")
                ),
                "required", List.of("query")
        );
    }

    @Override
    public String execute(Map<String, Object> args, Long userId) {
        String query = (String) args.get("query");
        if (!StringUtils.hasText(query)) {
            return "搜索关键词不能为空。";
        }
        String docType = args.containsKey("docType") ? (String) args.get("docType") : null;
        int topK = args.containsKey("topK") ? ((Number) args.get("topK")).intValue() : 4;

        List<Document> results = knowledgeRagQaSkill.search(String.valueOf(userId), query, docType, topK);
        if (results.isEmpty()) {
            return "知识库中未找到相关信息。用户可能还没有上传简历或进行过面试。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("检索到 ").append(results.size()).append(" 条相关片段：\n\n");
        for (int i = 0; i < results.size(); i++) {
            Document doc = results.get(i);
            String docTypeLabel = String.valueOf(doc.getMetadata().getOrDefault("docType", "未知"));
            String fileName = String.valueOf(doc.getMetadata().getOrDefault("fileName", ""));
            sb.append(i + 1).append(". [").append(docTypeLabel).append("] ").append(fileName).append("\n");
            String text = doc.getText();
            if (text != null && text.length() > 500) {
                text = text.substring(0, 500) + "...";
            }
            sb.append(text).append("\n\n");
        }
        return sb.toString();
    }
}
