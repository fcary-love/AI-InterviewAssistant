package com.faceai.pdfreader.service;

import com.faceai.pdfreader.model.record.*;
import com.faceai.pdfreader.repository.GamificationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class GamificationService {

    private final GamificationRepository repository;

    public GamificationService(GamificationRepository repository) {
        this.repository = repository;
    }

    // ============ 经验值与等级 ============

    /**
     * 面试结束后结算经验值
     */
    public InterviewSettleResult settleInterview(Long userId, Integer score, Integer questionCount,
                                                  Integer technicalScore, Integer expressionScore,
                                                  Integer matchScore, Integer problemSolvingScore,
                                                  Integer followUpScore, Integer stressResistanceScore) {
        repository.initUserGamification(userId);
        var gamification = repository.findByUserId(userId).orElseThrow();

        int expGained = 0;
        List<String> expBreakdown = new ArrayList<>();
        List<Achievement> unlockedAchievements = new ArrayList<>();

        // 1. 完成面试基础经验
        expGained += 50;
        expBreakdown.add("完成面试 +50");

        // 2. 答题经验
        int questionExp = questionCount * 10;
        expGained += questionExp;
        expBreakdown.add("答题 " + questionCount + " 道 +" + questionExp);

        // 3. 高分奖励
        if (score >= 90) {
            expGained += 30;
            expBreakdown.add("高分奖励 +30");
        }

        // 4. 更新统计数据
        int newTotalInterviews = gamification.totalInterviews() + 1;
        int newTotalQuestions = gamification.totalQuestions() + questionCount;
        int newBestScore = Math.max(gamification.bestScore(), score);

        // 5. 更新连续天数
        LocalDate today = LocalDate.now();
        int newStreakDays = gamification.streakDays();
        if (gamification.lastPracticeDate() == null) {
            newStreakDays = 1;
        } else if (gamification.lastPracticeDate().equals(today.minusDays(1))) {
            newStreakDays = gamification.streakDays() + 1;
        } else if (!gamification.lastPracticeDate().equals(today)) {
            newStreakDays = 1;
        }

        // 6. 连续天数奖励
        if (newStreakDays == 7) {
            expGained += 100;
            expBreakdown.add("连续7天 +100");
        } else if (newStreakDays == 30) {
            expGained += 200;
            expBreakdown.add("连续30天 +200");
        }

        // 7. 时段特殊成就
        int hour = LocalTime.now().getHour();
        if (hour >= 0 && hour < 6) {
            unlockedAchievements.addAll(checkSpecialAchievement(userId, "night_owl"));
        } else if (hour >= 6 && hour < 8) {
            unlockedAchievements.addAll(checkSpecialAchievement(userId, "early_bird"));
        }

        // 8. 计算新经验值和等级
        int newExp = gamification.expPoints() + expGained;
        int newLevel = calculateLevel(newExp);
        String newTitle = getTitle(newLevel);

        // 9. 检查连续天数成就
        if (newStreakDays >= 3) {
            unlockedAchievements.addAll(checkSpecialAchievement(userId, "streak_3"));
        }
        if (newStreakDays >= 7) {
            unlockedAchievements.addAll(checkSpecialAchievement(userId, "streak_7"));
        }
        if (newStreakDays >= 30) {
            unlockedAchievements.addAll(checkSpecialAchievement(userId, "streak_30"));
        }

        // 10. 检查面试次数成就
        unlockedAchievements.addAll(checkCountAchievement(userId, "total_interviews", newTotalInterviews));

        // 11. 检查分数成就
        if (score >= 90) {
            unlockedAchievements.addAll(checkSpecialAchievement(userId, "score_90"));
        }
        if (score >= 100) {
            unlockedAchievements.addAll(checkSpecialAchievement(userId, "score_perfect"));
        }

        // 12. 经验奖励
        for (var achievement : unlockedAchievements) {
            newExp += achievement.expReward();
            expBreakdown.add("解锁成就「" + achievement.name() + "」 +" + achievement.expReward());
        }

        // 13. 重新计算等级（可能因成就奖励升级）
        newLevel = calculateLevel(newExp);
        newTitle = getTitle(newLevel);

        // 14. 持久化
        repository.updateGamification(userId, newExp, newLevel, newTitle,
            newStreakDays, today, newTotalInterviews, newTotalQuestions, newBestScore);

        // 15. 更新每日任务
        repository.updateDailyTaskProgress(userId, today, "interview", 1);
        repository.updateDailyTaskProgress(userId, today, "questions", questionCount);
        if (score >= 80) {
            repository.updateDailyTaskProgress(userId, today, "score", 1);
        }

        // 16. 初始化今日任务（如果还没有）
        initDailyTasks(userId, today);

        return new InterviewSettleResult(
            expGained, newExp, newLevel, newTitle,
            expBreakdown, unlockedAchievements,
            newStreakDays, gamification.level(), newLevel > gamification.level()
        );
    }

    /**
     * 获取用户游戏化概览
     */
    public GamificationSummary getSummary(Long userId) {
        repository.initUserGamification(userId);
        var gamification = repository.findByUserId(userId).orElseThrow();

        int expToNextLevel = getExpForLevel(gamification.level() + 1);
        int expForCurrentLevel = getExpForLevel(gamification.level());
        int expProgress = gamification.expPoints() - expForCurrentLevel;
        int expNeeded = expToNextLevel - expForCurrentLevel;

        // 最近解锁的成就
        var userAchievements = repository.findUserAchievements(userId);
        var allAchievements = repository.findAllAchievements();
        var recentAchievements = userAchievements.stream()
            .limit(5)
            .map(ua -> allAchievements.stream()
                .filter(a -> a.id().equals(ua.achievementId()))
                .findFirst().orElse(null))
            .filter(a -> a != null)
            .toList();

        // 今日任务
        var dailyTasks = repository.findDailyTasks(userId, LocalDate.now());
        if (dailyTasks.isEmpty()) {
            initDailyTasks(userId, LocalDate.now());
            dailyTasks = repository.findDailyTasks(userId, LocalDate.now());
        }

        return new GamificationSummary(
            gamification.expPoints(),
            gamification.level(),
            gamification.title(),
            gamification.streakDays(),
            gamification.totalInterviews(),
            gamification.totalQuestions(),
            gamification.bestScore(),
            expNeeded,
            expProgress,
            recentAchievements,
            dailyTasks
        );
    }

    /**
     * 获取所有成就（含用户解锁状态）
     */
    public List<AchievementWithStatus> getAchievements(Long userId) {
        var allAchievements = repository.findAllAchievements();
        var userAchievements = repository.findUserAchievements(userId);

        return allAchievements.stream().map(achievement -> {
            var userAchievement = userAchievements.stream()
                .filter(ua -> ua.achievementId().equals(achievement.id()))
                .findFirst()
                .orElse(null);
            return new AchievementWithStatus(
                achievement,
                userAchievement != null,
                userAchievement != null ? userAchievement.unlockedAt() : null
            );
        }).toList();
    }

    /**
     * 领取每日任务奖励
     */
    public int claimDailyTask(Long userId, String taskType) {
        LocalDate today = LocalDate.now();
        var tasks = repository.findDailyTasks(userId, today);
        var task = tasks.stream()
            .filter(t -> t.taskType().equals(taskType) && t.completed() && !t.claimed())
            .findFirst()
            .orElse(null);

        if (task == null) {
            return 0;
        }

        repository.claimDailyTask(userId, today, taskType);
        repository.addExp(userId, task.expReward());
        return task.expReward();
    }

    // ============ 内部方法 ============

    private void initDailyTasks(Long userId, LocalDate date) {
        var existing = repository.findDailyTasks(userId, date);
        if (!existing.isEmpty()) return;

        repository.createDailyTask(new DailyTask(null, userId, date, "interview", "完成1场模拟面试", 1, 0, false, false, 50));
        repository.createDailyTask(new DailyTask(null, userId, date, "questions", "回答5道面试题", 5, 0, false, false, 30));
        repository.createDailyTask(new DailyTask(null, userId, date, "score", "获得1次80分以上", 1, 0, false, false, 20));
    }

    private List<Achievement> checkSpecialAchievement(Long userId, String code) {
        var achievements = repository.findAllAchievements();
        var achievement = achievements.stream()
            .filter(a -> a.code().equals(code))
            .findFirst()
            .orElse(null);

        if (achievement == null || repository.hasAchievement(userId, achievement.id())) {
            return List.of();
        }

        repository.unlockAchievement(userId, achievement.id());
        return List.of(achievement);
    }

    private List<Achievement> checkCountAchievement(Long userId, String conditionType, int currentValue) {
        var achievements = repository.findAllAchievements();
        var unlocked = new ArrayList<Achievement>();

        for (var achievement : achievements) {
            if (!achievement.conditionType().equals(conditionType)) continue;
            if (currentValue < achievement.conditionValue()) continue;
            if (repository.hasAchievement(userId, achievement.id())) continue;

            repository.unlockAchievement(userId, achievement.id());
            unlocked.add(achievement);
        }

        return unlocked;
    }

    public static int calculateLevel(int exp) {
        return (int) Math.floor(Math.sqrt(exp / 100.0)) + 1;
    }

    public static int getExpForLevel(int level) {
        return (int) Math.pow(level - 1, 2) * 100;
    }

    public static String getTitle(int level) {
        if (level >= 91) return "面试之神";
        if (level >= 71) return "面试王者";
        if (level >= 51) return "面试大师";
        if (level >= 31) return "面试专家";
        if (level >= 16) return "面试达人";
        if (level >= 6) return "面试学徒";
        return "面试新手";
    }

    // ============ 返回类型 ============

    public record InterviewSettleResult(
        int expGained,
        int totalExp,
        int level,
        String title,
        List<String> expBreakdown,
        List<Achievement> unlockedAchievements,
        int streakDays,
        int previousLevel,
        boolean levelUp
    ) {}

    public record AchievementWithStatus(
        Achievement achievement,
        boolean unlocked,
        java.time.LocalDateTime unlockedAt
    ) {}
}
