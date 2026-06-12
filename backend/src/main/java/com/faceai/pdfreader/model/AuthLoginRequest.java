package com.faceai.pdfreader.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthLoginRequest(
        @NotBlank(message = "账号不能为空")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 60, message = "密码长度应为 6-60 位")
        String password
) {
}
