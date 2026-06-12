package com.faceai.pdfreader.model;

import jakarta.validation.constraints.NotBlank;

public record ImageDescriptionRequest(
        @NotBlank(message = "图片地址不能为空")
        String imageUrl
) {
}
