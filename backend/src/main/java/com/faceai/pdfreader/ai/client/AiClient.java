package com.faceai.pdfreader.ai.client;

import com.faceai.pdfreader.config.AiProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

@Component
public class AiClient {

    private final WebClient aiWebClient;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiClient(WebClient aiWebClient, AiProperties aiProperties) {
        this.aiWebClient = aiWebClient;
        this.aiProperties = aiProperties;
    }

    // ==================== 原有方法（向后兼容） ====================

    public String chat(String prompt) {
        List<Map<String, Object>> messages = List.of(Map.of("role", "user", "content", prompt));
        return chatWithMessages(messages);
    }

    public String multimodal(List<Map<String, Object>> content) {
        Map<String, Object> payload = Map.of(
                "model", resolveVisionModel(),
                "temperature", defaultTemperature(),
                "max_tokens", defaultMaxTokens(),
                "messages", List.of(Map.of("role", "user", "content", content))
        );
        CompletionResponse response = invokeChatCompletionRaw(payload);
        String text = response.choices().get(0).message().content();
        if (!StringUtils.hasText(text)) {
            throw new IllegalArgumentException("AI 接口返回内容为空");
        }
        return text.trim();
    }

    public Flux<String> streamChat(String prompt) {
        List<Map<String, Object>> messages = List.of(Map.of("role", "user", "content", prompt));
        return streamChatWithMessages(messages);
    }

    // ==================== 新方法：多轮消息 ====================

    public String chatWithMessages(List<Map<String, Object>> messages) {
        Map<String, Object> payload = Map.of(
                "model", resolveTextModel(),
                "temperature", defaultTemperature(),
                "max_tokens", defaultMaxTokens(),
                "messages", messages
        );
        CompletionResponse response = invokeChatCompletionRaw(payload);
        String content = response.choices().get(0).message().content();
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("AI 接口返回内容为空");
        }
        return content.trim();
    }

    public Flux<String> streamChatWithMessages(List<Map<String, Object>> messages) {
        Map<String, Object> payload = Map.of(
                "model", resolveTextModel(),
                "temperature", defaultTemperature(),
                "max_tokens", defaultMaxTokens(),
                "stream", true,
                "messages", messages
        );
        return streamChatCompletion(payload);
    }

    // ==================== 新方法：Tool Calling ====================

    /**
     * 发送消息 + 工具定义，返回完整响应（含 tool_calls）。
     * 调用方负责检查 finishReason 并处理 tool_calls。
     */
    public ChatResult chatWithTools(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("model", resolveTextModel());
        payload.put("temperature", defaultTemperature());
        payload.put("max_tokens", defaultMaxTokens());
        payload.put("messages", messages);
        payload.put("tools", tools);

        CompletionResponse response = invokeChatCompletionRaw(payload);
        CompletionChoice choice = response.choices().get(0);

        String content = choice.message().content();
        List<ToolCallResult> toolCalls = choice.message().toolCalls();
        String finishReason = choice.finishReason();

        return new ChatResult(
                StringUtils.hasText(content) ? content.trim() : null,
                toolCalls != null ? toolCalls : List.of(),
                finishReason
        );
    }

    public void validateConfiguration() {
        if (!StringUtils.hasText(aiProperties.baseUrl())
                || !StringUtils.hasText(aiProperties.apiKey())
                || !StringUtils.hasText(aiProperties.model())) {
            throw new IllegalArgumentException("AI 配置不完整，请检查 app.ai.base-url、app.ai.api-key、app.ai.model");
        }
    }

    // ==================== 内部实现 ====================

    private CompletionResponse invokeChatCompletionRaw(Map<String, Object> payload) {
        CompletionResponse response;
        try {
            response = aiWebClient.post()
                    .uri(resolveChatPath())
                    .bodyValue(payload)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .map(body -> new IllegalArgumentException("AI 接口调用失败: " + body))
                    )
                    .bodyToMono(CompletionResponse.class)
                    .timeout(Duration.ofSeconds(90))
                    .retryWhen(Retry.backoff(2, Duration.ofMillis(600)).filter(this::isRetryable))
                    .block();
        } catch (WebClientRequestException ex) {
            throw new IllegalArgumentException("AI 服务连接不稳定，请稍后重试。原因：" + friendlyNetworkMessage(ex));
        } catch (RuntimeException ex) {
            if (isRetryExhausted(ex)) {
                throw new IllegalArgumentException("AI 服务多次连接失败，请稍后重试。当前网络或模型服务可能不稳定。");
            }
            throw ex;
        }

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalArgumentException("AI 接口未返回有效内容");
        }
        return response;
    }

    private Flux<String> streamChatCompletion(Map<String, Object> payload) {
        return aiWebClient.post()
                .uri(resolveChatPath())
                .bodyValue(payload)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(line -> line != null && !line.isBlank())
                .mapNotNull(this::extractStreamContent)
                .filter(Objects::nonNull)
                .timeout(Duration.ofSeconds(120))
                .onErrorResume(WebClientRequestException.class, ex ->
                        Flux.error(new IllegalArgumentException("AI 服务连接不稳定，请稍后重试。")));
    }

    private String extractStreamContent(String sseLine) {
        if (sseLine == null) return null;
        String trimmed = sseLine.trim();
        if (!trimmed.startsWith("data:")) return null;
        String json = trimmed.substring(5).trim();
        if ("[DONE]".equals(json)) return null;
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && !choices.isEmpty()) {
                JsonNode delta = choices.get(0).get("delta");
                if (delta != null && delta.has("content")) {
                    String content = delta.get("content").asText();
                    if (!content.isEmpty()) {
                        return content;
                    }
                }
            }
        } catch (Exception ignored) {
            // skip malformed chunks
        }
        return null;
    }

    private String resolveChatPath() {
        return StringUtils.hasText(aiProperties.chatPath()) ? aiProperties.chatPath() : "/chat/completions";
    }

    private boolean isRetryable(Throwable throwable) {
        return throwable instanceof WebClientRequestException
                || throwable instanceof java.util.concurrent.TimeoutException;
    }

    private boolean isRetryExhausted(RuntimeException ex) {
        return ex.getClass().getName().contains("RetryExhausted");
    }

    private String friendlyNetworkMessage(WebClientRequestException ex) {
        String message = ex.getMessage();
        if (message != null && message.toLowerCase().contains("connection reset")) {
            return "大模型接口主动断开连接";
        }
        return StringUtils.hasText(message) ? message : "网络请求失败";
    }

    private double defaultTemperature() {
        return aiProperties.temperature() != null ? aiProperties.temperature() : 0.3D;
    }

    private int defaultMaxTokens() {
        return aiProperties.maxTokens() != null ? aiProperties.maxTokens() : 1200;
    }

    private String resolveTextModel() {
        return StringUtils.hasText(aiProperties.textModel()) ? aiProperties.textModel() : aiProperties.model();
    }

    private String resolveVisionModel() {
        return StringUtils.hasText(aiProperties.visionModel()) ? aiProperties.visionModel() : aiProperties.model();
    }

    // ==================== 响应数据结构 ====================

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CompletionResponse(List<CompletionChoice> choices) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CompletionChoice(CompletionMessage message, @JsonProperty("finish_reason") String finishReason) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CompletionMessage(String content, @JsonProperty("tool_calls") List<ToolCallResult> toolCalls) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ToolCallResult(String id, String type, ToolCallFunction function) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ToolCallFunction(String name, String arguments) {}

    /**
     * chatWithTools 的返回结果。
     */
    public record ChatResult(String content, List<ToolCallResult> toolCalls, String finishReason) {
        public boolean hasToolCalls() {
            return toolCalls != null && !toolCalls.isEmpty();
        }

        public boolean isStop() {
            return "stop".equals(finishReason);
        }
    }
}
