package com.faceai.pdfreader.ai.agent;

import com.faceai.pdfreader.ai.client.AiClient;
import com.faceai.pdfreader.ai.prompts.AgentPrompts;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AgentEngine {

    private static final int MAX_ITERATIONS = 10;
    private static final Logger log = LoggerFactory.getLogger(AgentEngine.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final AiClient aiClient;
    private final List<AgentTool> tools;
    private final Map<String, AgentTool> toolMap;

    public AgentEngine(AiClient aiClient, List<AgentTool> tools) {
        this.aiClient = aiClient;
        this.tools = tools;
        this.toolMap = new HashMap<>();
        for (AgentTool tool : tools) {
            toolMap.put(tool.name(), tool);
        }
    }

    public AgentResponse run(List<Map<String, Object>> history, Long userId) {
        List<Map<String, Object>> messages = new ArrayList<>();

        // System prompt
        messages.add(Map.of("role", "system", "content", AgentPrompts.buildSystemPromptWithTools(tools)));

        // History
        messages.addAll(history);

        // Tool definitions
        List<Map<String, Object>> toolDefinitions = AgentPrompts.buildToolDefinitions(tools);

        List<AgentToolCall> allToolCalls = new ArrayList<>();
        String finalAnswer = null;

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            AiClient.ChatResult result = aiClient.chatWithTools(messages, toolDefinitions);

            if (result.isStop()) {
                finalAnswer = result.content();
                break;
            }

            if (result.hasToolCalls()) {
                // Add assistant message with tool calls
                Map<String, Object> assistantMsg = new HashMap<>();
                assistantMsg.put("role", "assistant");
                if (result.content() != null) {
                    assistantMsg.put("content", result.content());
                }
                List<Map<String, Object>> toolCallsMsg = new ArrayList<>();
                for (AiClient.ToolCallResult tc : result.toolCalls()) {
                    toolCallsMsg.add(Map.of(
                            "id", tc.id(),
                            "type", "function",
                            "function", Map.of(
                                    "name", tc.function().name(),
                                    "arguments", tc.function().arguments()
                            )
                    ));
                }
                assistantMsg.put("tool_calls", toolCallsMsg);
                messages.add(assistantMsg);

                // Execute each tool call
                for (AiClient.ToolCallResult tc : result.toolCalls()) {
                    String toolName = tc.function().name();
                    String toolArgs = tc.function().arguments();
                    AgentTool tool = toolMap.get(toolName);

                    String toolResult;
                    if (tool == null) {
                        toolResult = "错误：工具 " + toolName + " 不存在。";
                    } else {
                        try {
                            Map<String, Object> args = parseArguments(toolArgs);
                            toolResult = tool.execute(args, userId);
                        } catch (Exception e) {
                            log.warn("Tool {} execution failed: {}", toolName, e.getMessage());
                            toolResult = "工具执行出错：" + e.getMessage();
                        }
                    }

                    allToolCalls.add(new AgentToolCall(toolName, toolArgs, toolResult));

                    // Add tool result message
                    messages.add(Map.of(
                            "role", "tool",
                            "tool_call_id", tc.id(),
                            "content", toolResult
                    ));
                }
            } else {
                // No tool calls and not stop - treat as final answer
                finalAnswer = result.content();
                break;
            }
        }

        if (finalAnswer == null) {
            finalAnswer = "抱歉，我在处理过程中遇到了问题，请重新描述你的问题。";
        }

        return new AgentResponse(finalAnswer, allToolCalls);
    }

    private Map<String, Object> parseArguments(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return mapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse tool arguments: {}", json);
            return Map.of();
        }
    }

    public record AgentResponse(String content, List<AgentToolCall> toolCalls) {}

    public record AgentToolCall(String toolName, String arguments, String result) {}
}
