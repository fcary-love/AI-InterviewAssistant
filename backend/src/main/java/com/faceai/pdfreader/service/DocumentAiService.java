package com.faceai.pdfreader.service;

import com.faceai.pdfreader.ai.client.AiClient;
import com.faceai.pdfreader.model.ChatHistoryMessage;
import com.faceai.pdfreader.model.DocumentQaResponse;
import com.faceai.pdfreader.model.DocumentSummaryResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

@Service
public class DocumentAiService {

    private static final int MAX_CONTEXT_LENGTH = 8_000;

    private final AiClient aiClient;
    private final DocumentService documentService;

    public DocumentAiService(AiClient aiClient, DocumentService documentService) {
        this.aiClient = aiClient;
        this.documentService = documentService;
    }

    public DocumentSummaryResponse summarize(String fileId) {
        aiClient.validateConfiguration();
        String sourceText = resolveText(fileId);
        String prompt = """
                你是一名求职简历顾问。请根据下面的材料生成一份简洁的中文分析。
                输出包括：
                1. 候选人画像
                2. 主要技能和经历
                3. 简历亮点
                4. 需要补强或修改的地方
                请使用自然正文格式，不要使用 Markdown 标题、星号、井号、代码块或表情符号。

                材料：
                %s
                """.formatted(limitText(sourceText));
        return new DocumentSummaryResponse(aiClient.chat(prompt));
    }

    public DocumentQaResponse answerQuestion(String fileId, String question, List<ChatHistoryMessage> history) {
        aiClient.validateConfiguration();
        String sourceText = resolveText(fileId);
        String prompt = """
                你是一名面试准备助手。请优先根据用户上传的材料回答问题。
                回答要求：
                1. 用户问简历修改时，给出具体修改建议
                2. 用户问岗位匹配时，说明匹配点和风险点
                3. 用户问材料依据时，指出依据来自材料中的哪些信息
                4. 材料不足时明确说明，不要编造
                5. 请使用自然正文格式，不要使用 Markdown 标题、星号、井号、代码块或表情符号
                6. 回答尽量控制在 800 字以内；如果用户要求完整重写简历，先给整体改法和最重要的 3-5 处示例，避免一次性输出过长

                最近对话：
                %s

                用户问题：
                %s

                材料：
                %s
                """.formatted(formatHistory(history), question, limitText(sourceText));
        return new DocumentQaResponse(aiClient.chat(prompt));
    }

    public Flux<String> streamAnswer(String fileId, String question, List<ChatHistoryMessage> history) {
        aiClient.validateConfiguration();
        String sourceText = resolveText(fileId);
        String prompt = """
                你是一名面试准备助手。请优先根据用户上传的材料回答问题。
                回答要求：
                1. 用户问简历修改时，给出具体修改建议
                2. 用户问岗位匹配时，说明匹配点和风险点
                3. 用户问材料依据时，指出依据来自材料中的哪些信息
                4. 材料不足时明确说明，不要编造
                5. 请使用自然正文格式，不要使用 Markdown 标题、星号、井号、代码块或表情符号
                6. 回答尽量控制在 800 字以内；如果用户要求完整重写简历，先给整体改法和最重要的 3-5 处示例，避免一次性输出过长

                最近对话：
                %s

                用户问题：
                %s

                材料：
                %s
                """.formatted(formatHistory(history), question, limitText(sourceText));
        return aiClient.streamChat(prompt);
    }

    private String resolveText(String fileId) {
        String sourceText = documentService.readText(fileId);
        if (!StringUtils.hasText(sourceText)) {
            throw new IllegalArgumentException("当前文档没有可分析的文本内容");
        }
        return sourceText;
    }

    private String limitText(String text) {
        if (text.length() <= MAX_CONTEXT_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_CONTEXT_LENGTH);
    }

    private String formatHistory(List<ChatHistoryMessage> history) {
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

    private String limitHistoryText(String text) {
        String normalized = text.strip();
        if (normalized.length() <= 360) {
            return normalized;
        }
        return normalized.substring(0, 360);
    }
}
