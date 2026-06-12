package com.faceai.pdfreader.service;

import com.faceai.pdfreader.ai.agent.AgentEngine;
import com.faceai.pdfreader.repository.AgentConversationRepository;
import com.faceai.pdfreader.repository.AgentConversationRepository.AgentMessageRecord;
import com.faceai.pdfreader.repository.AgentConversationRepository.AgentSessionRecord;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AgentService {

    private final AgentEngine agentEngine;
    private final AgentConversationRepository conversationRepository;

    public AgentService(AgentEngine agentEngine, AgentConversationRepository conversationRepository) {
        this.agentEngine = agentEngine;
        this.conversationRepository = conversationRepository;
    }

    public AgentChatResponse chat(Long userId, String sessionId, String message) {
        // Ensure session exists
        String activeSessionId = ensureSession(userId, sessionId, message);

        // Save user message
        conversationRepository.saveMessage(activeSessionId, "user", message, null, null, null);

        // Build history from DB
        List<Map<String, Object>> history = buildHistory(activeSessionId);

        // Run agent
        AgentEngine.AgentResponse response = agentEngine.run(history, userId);

        // Save assistant response
        conversationRepository.saveMessage(activeSessionId, "assistant", response.content(), null, null, null);

        // Save tool call records
        for (AgentEngine.AgentToolCall tc : response.toolCalls()) {
            conversationRepository.saveMessage(activeSessionId, "tool", tc.result(), tc.toolName(), null, tc.arguments());
        }

        // Update session title from first user message
        if (isNewSession(sessionId)) {
            String title = message.length() > 50 ? message.substring(0, 50) + "..." : message;
            conversationRepository.updateSessionTitle(activeSessionId, title);
        }

        List<AgentToolCallResponse> toolCallResponses = response.toolCalls().stream()
                .map(tc -> new AgentToolCallResponse(tc.toolName(), tc.arguments(), tc.result()))
                .toList();

        return new AgentChatResponse(activeSessionId, response.content(), toolCallResponses);
    }

    public List<AgentSessionResponse> listSessions(Long userId) {
        return conversationRepository.listSessions(userId).stream()
                .map(s -> new AgentSessionResponse(s.sessionId(), s.title(), s.status(), s.updatedAt()))
                .toList();
    }

    public List<AgentMessageResponse> getSessionMessages(Long userId, String sessionId) {
        if (!conversationRepository.sessionExists(userId, sessionId)) {
            throw new IllegalArgumentException("会话不存在");
        }
        return conversationRepository.findMessages(sessionId).stream()
                .map(m -> new AgentMessageResponse(m.role(), m.content(), m.toolName(), m.toolCallId(), m.toolArguments(), m.createdAt()))
                .toList();
    }

    public boolean deleteSession(Long userId, String sessionId) {
        return conversationRepository.deleteSession(userId, sessionId);
    }

    private String ensureSession(Long userId, String sessionId, String message) {
        if (StringUtils.hasText(sessionId) && conversationRepository.sessionExists(userId, sessionId)) {
            return sessionId;
        }
        String title = message.length() > 30 ? message.substring(0, 30) + "..." : message;
        return conversationRepository.createSession(userId, title);
    }

    private boolean isNewSession(String sessionId) {
        return !StringUtils.hasText(sessionId);
    }

    private List<Map<String, Object>> buildHistory(String sessionId) {
        List<AgentMessageRecord> records = conversationRepository.findMessages(sessionId);
        List<Map<String, Object>> history = new ArrayList<>();
        for (AgentMessageRecord record : records) {
            if ("user".equals(record.role())) {
                history.add(Map.of("role", "user", "content", record.content() != null ? record.content() : ""));
            } else if ("assistant".equals(record.role())) {
                history.add(Map.of("role", "assistant", "content", record.content() != null ? record.content() : ""));
            }
            // Skip tool messages in history - they are reconstructed by the engine
        }
        // Keep last 20 messages to avoid context overflow
        if (history.size() > 20) {
            history = new ArrayList<>(history.subList(history.size() - 20, history.size()));
        }
        return history;
    }

    // Response DTOs
    public record AgentChatResponse(String sessionId, String content, List<AgentToolCallResponse> toolCalls) {}

    public record AgentToolCallResponse(String toolName, String arguments, String result) {}

    public record AgentSessionResponse(String sessionId, String title, String status, String updatedAt) {}

    public record AgentMessageResponse(String role, String content, String toolName, String toolCallId, String toolArguments, String createdAt) {}

    // Request DTO
    public record AgentChatRequest(String sessionId, String message) {}
}
