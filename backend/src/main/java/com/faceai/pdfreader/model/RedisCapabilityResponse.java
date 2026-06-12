package com.faceai.pdfreader.model;

public record RedisCapabilityResponse(
        boolean reachable,
        boolean vectorSearchReady,
        String message
) {
}
