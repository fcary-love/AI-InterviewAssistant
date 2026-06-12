package com.faceai.pdfreader.ai.prompts;

import com.faceai.pdfreader.model.ChatHistoryMessage;
import java.util.List;
import org.springframework.util.StringUtils;

public final class PromptTemplates {

    private PromptTemplates() {
    }

    public static String summaryPrompt(String sourceText) {
        return """
                你是一名文档分析助手。请基于下面的 PDF 内容生成一份清晰的中文摘要。
                输出要求：
                1. 先用 1 段话概述主题
                2. 再列出 3-6 个关键点
                3. 如果内容不足，请明确说明

                PDF 内容：
                %s
                """.formatted(sourceText);
    }

    public static String qaPrompt(String sourceText, String question, List<ChatHistoryMessage> history) {
        return """
                你是一名文档问答助手。请严格基于提供的 PDF 内容回答用户问题。
                规则：
                1. 如果答案能从文档中得到，直接用中文作答
                2. 如果文档信息不足，明确说“文档中未提供足够信息”
                3. 不要编造
                4. 回答尽量控制在 800 字以内，优先给结论和可执行建议

                最近对话：
                %s

                用户问题：
                %s

                PDF 内容：
                %s
                """.formatted(formatHistory(history), question, sourceText);
    }

    public static String imageDescriptionPrompt() {
        return "请用中文说明这张图片的主要内容、可能的场景和关键信息。如果图片中文字清晰，也请顺带提取。";
    }

    private static String formatHistory(List<ChatHistoryMessage> history) {
        if (history == null || history.isEmpty()) {
            return "暂无历史对话。";
        }
        return history.stream()
                .filter(message -> message != null && StringUtils.hasText(message.content()))
                .limit(6)
                .map(message -> {
                    String role = "user".equalsIgnoreCase(message.role()) ? "用户" : "助手";
                    return role + "：" + limitHistoryText(message.content());
                })
                .reduce((left, right) -> left + "\n" + right)
                .orElse("暂无历史对话。");
    }

    private static String limitHistoryText(String text) {
        String normalized = text.strip();
        if (normalized.length() <= 360) {
            return normalized;
        }
        return normalized.substring(0, 360);
    }
}
