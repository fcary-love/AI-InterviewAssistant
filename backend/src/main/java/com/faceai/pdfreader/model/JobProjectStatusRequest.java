package com.faceai.pdfreader.model;

import jakarta.validation.constraints.NotBlank;

public record JobProjectStatusRequest(
        @NotBlank(message = "请选择项目状态")
        String status
) {
}
