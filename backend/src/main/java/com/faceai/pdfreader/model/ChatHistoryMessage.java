package com.faceai.pdfreader.model;

public record ChatHistoryMessage(
        String role,
        String content
) {
}
