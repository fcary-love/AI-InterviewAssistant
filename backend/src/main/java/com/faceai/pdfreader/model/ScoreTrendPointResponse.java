package com.faceai.pdfreader.model;

public record ScoreTrendPointResponse(
        String label,
        Integer score,
        String date
) {
}
