package com.faceai.pdfreader.ai.agent;

import java.util.Map;

public interface AgentTool {

    String name();

    String description();

    Map<String, Object> jsonSchema();

    String execute(Map<String, Object> args, Long userId);
}
