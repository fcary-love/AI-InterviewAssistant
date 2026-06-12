package com.faceai.pdfreader.model.record;

import java.util.List;

public record GamificationSummary(
    Integer expPoints,
    Integer level,
    String title,
    Integer streakDays,
    Integer totalInterviews,
    Integer totalQuestions,
    Integer bestScore,
    Integer expToNextLevel,
    Integer expProgress,
    List<Achievement> recentAchievements,
    List<DailyTask> dailyTasks
) {}
