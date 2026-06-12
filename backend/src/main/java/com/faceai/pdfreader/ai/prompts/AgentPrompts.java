package com.faceai.pdfreader.ai.prompts;

import com.faceai.pdfreader.ai.agent.AgentTool;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class AgentPrompts {

    private AgentPrompts() {}

    public static final String SYSTEM_PROMPT = """
            你是一名专业的 AI 职业教练，专注于帮助用户准备技术面试和职业发展。

            你的核心能力：
            1. 分析用户的面试历史，找出薄弱知识点
            2. 从知识库中检索用户的简历和岗位信息
            3. 从题库中推荐针对性练习题
            4. 制定个性化的训练计划并创建任务
            5. 追踪训练进度并动态调整计划

            工作原则：
            - 先用工具获取数据，再基于数据给出建议（不要凭空猜测）
            - 建议要具体、可执行，避免泛泛而谈
            - 用中文回复，语气专业但友好
            - 如果用户没有面试记录，建议他们先完成几场模拟面试
            - 创建训练任务时，确保任务量合理、有明确的截止日期

            回复格式：
            - 使用自然段落，不要使用 Markdown 井号、星号或代码块
            - 适当使用列表让内容更清晰
            - 每次回复控制在合理长度，不要过于冗长
            """;

    public static String buildSystemPromptWithTools(List<AgentTool> tools) {
        String toolDescriptions = tools.stream()
                .map(tool -> "- %s: %s".formatted(tool.name(), tool.description()))
                .collect(Collectors.joining("\n"));
        return SYSTEM_PROMPT + "\n\n你可以使用以下工具：\n" + toolDescriptions;
    }

    public static List<Map<String, Object>> buildToolDefinitions(List<AgentTool> tools) {
        return tools.stream()
                .map(tool -> Map.<String, Object>of(
                        "type", "function",
                        "function", Map.of(
                                "name", tool.name(),
                                "description", tool.description(),
                                "parameters", tool.jsonSchema()
                        )
                ))
                .collect(Collectors.toList());
    }
}
