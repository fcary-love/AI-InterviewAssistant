package com.faceai.pdfreader.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AuthRegisterRequest(
        @NotBlank(message = "账号不能为空")
        @Pattern(regexp = "^[A-Za-z0-9_]{4,30}$", message = "账号只能使用 4-30 位字母、数字或下划线")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 60, message = "密码长度应为 6-60 位")
        String password,

        @NotBlank(message = "昵称不能为空")
        @Size(max = 40, message = "昵称不能超过 40 位")
        String displayName
) {
}
