package com.faceai.pdfreader.model;

import jakarta.validation.constraints.NotBlank;

public record TrainingTaskStatusRequest(
        @NotBlank(message = "任务状态不能为空")
        String status
) {
}
