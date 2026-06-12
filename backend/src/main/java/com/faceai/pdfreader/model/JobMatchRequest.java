package com.faceai.pdfreader.model;

import jakarta.validation.constraints.NotBlank;

public record JobMatchRequest(
        @NotBlank(message = "请先选择简历文件")
        String resumeFileId,

        @NotBlank(message = "请粘贴岗位 JD")
        String jdText
) {
}
