package com.faceai.pdfreader.model.record;

import java.time.LocalDate;

public record DailyTask(
    Long id,
    Long userId,
    LocalDate taskDate,
    String taskType,
    String description,
    Integer targetCount,
    Integer currentCount,
    Boolean completed,
    Boolean claimed,
    Integer expReward
) {}
