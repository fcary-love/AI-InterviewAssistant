package com.faceai.pdfreader.model.record;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserGamification(
    Long userId,
    Integer expPoints,
    Integer level,
    String title,
    Integer streakDays,
    LocalDate lastPracticeDate,
    Integer totalInterviews,
    Integer totalQuestions,
    Integer bestScore,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
