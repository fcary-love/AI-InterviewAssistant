package com.faceai.pdfreader.model;

public record TrainingTaskResponse(
        Long id,
        String title,
        String description,
        String taskType,
        String category,
        Integer targetCount,
        Integer finishedCount,
        String status,
        String source,
        String dueDate,
        String createdAt,
        String updatedAt
) {
}
