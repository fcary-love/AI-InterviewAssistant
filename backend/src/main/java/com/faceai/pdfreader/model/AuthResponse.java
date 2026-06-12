package com.faceai.pdfreader.model;

public record AuthResponse(
        String token,
        AuthUserResponse user
) {
}
