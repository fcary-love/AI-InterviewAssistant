package com.faceai.pdfreader.model;

public record AuthUserResponse(
        Long id,
        String username,
        String displayName
) {
}
