package com.faceai.pdfreader.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record JobProjectCreateRequest(
        @NotBlank(message = "请填写公司名称")
        String companyName,
        @NotBlank(message = "请填写岗位名称")
        String jobTitle,
        @NotBlank(message = "请填写岗位 JD")
        String jdText,
        @NotNull(message = "请选择绑定的简历版本")
        Long resumeVersionId
) {
}
