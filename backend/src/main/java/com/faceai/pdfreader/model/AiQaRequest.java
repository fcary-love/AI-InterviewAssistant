package com.faceai.pdfreader.model;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record AiQaRequest(
        @NotBlank(message = "问题不能为空")
        String question,
        List<ChatHistoryMessage> history
) {
}
