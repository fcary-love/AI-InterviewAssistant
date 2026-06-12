package com.faceai.pdfreader.controller;

import com.faceai.pdfreader.auth.AuthContext;
import com.faceai.pdfreader.model.ApiResponse;
import com.faceai.pdfreader.service.AgentService;
import com.faceai.pdfreader.service.AgentService.AgentChatRequest;
import com.faceai.pdfreader.service.AgentService.AgentChatResponse;
import com.faceai.pdfreader.service.AgentService.AgentMessageResponse;
import com.faceai.pdfreader.service.AgentService.AgentSessionResponse;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/chat")
    public ApiResponse<AgentChatResponse> chat(@RequestBody AgentChatRequest request) {
        Long userId = AuthContext.currentUserId();
        AgentChatResponse response = agentService.chat(userId, request.sessionId(), request.message());
        return ApiResponse.success(response);
    }

    @GetMapping("/sessions")
    public ApiResponse<List<AgentSessionResponse>> listSessions() {
        Long userId = AuthContext.currentUserId();
        return ApiResponse.success(agentService.listSessions(userId));
    }

    @GetMapping("/sessions/{sessionId}")
    public ApiResponse<List<AgentMessageResponse>> getSessionMessages(@PathVariable String sessionId) {
        Long userId = AuthContext.currentUserId();
        return ApiResponse.success(agentService.getSessionMessages(userId, sessionId));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ApiResponse<Void> deleteSession(@PathVariable String sessionId) {
        Long userId = AuthContext.currentUserId();
        agentService.deleteSession(userId, sessionId);
        return ApiResponse.success(null);
    }
}
