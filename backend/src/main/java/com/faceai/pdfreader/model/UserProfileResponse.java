package com.faceai.pdfreader.model;

public record UserProfileResponse(
        Long id,
        String displayName,
        String targetRole,
        String skillKeywords,
        String createdAt
) {
}
