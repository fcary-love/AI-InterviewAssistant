package com.faceai.pdfreader.model;

import java.util.List;

public record TrainingGenerateResponse(
        Integer created,
        String message,
        List<TrainingTaskResponse> tasks
) {
}
