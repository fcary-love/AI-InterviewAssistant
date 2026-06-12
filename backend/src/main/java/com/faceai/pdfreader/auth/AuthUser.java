package com.faceai.pdfreader.auth;

public record AuthUser(
        Long id,
        String username,
        String displayName
) {
}
