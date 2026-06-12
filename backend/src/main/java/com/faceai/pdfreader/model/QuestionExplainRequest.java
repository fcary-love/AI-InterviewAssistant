package com.faceai.pdfreader.model;

import jakarta.validation.constraints.NotBlank;

public record QuestionExplainRequest(
        @NotBlank(message = "问题不能为空")
        String question
) {
}
