package com.faceai.pdfreader.model;

public record WeakPointResponse(
        String name,
        Integer count,
        String suggestion
) {
}
