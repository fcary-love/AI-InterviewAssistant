package com.faceai.pdfreader.model.record;

import java.time.LocalDateTime;

public record Achievement(
    Long id,
    String code,
    String name,
    String description,
    String icon,
    String category,
    String conditionType,
    Integer conditionValue,
    Integer expReward,
    String rarity,
    LocalDateTime createdAt
) {}
