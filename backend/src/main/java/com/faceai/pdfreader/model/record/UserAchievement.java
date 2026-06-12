package com.faceai.pdfreader.model.record;

import java.time.LocalDateTime;

public record UserAchievement(
    Long id,
    Long userId,
    Long achievementId,
    LocalDateTime unlockedAt
) {}
