package com.faceai.pdfreader.repository;

import com.faceai.pdfreader.model.record.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class GamificationRepository {

    private final JdbcTemplate jdbc;

    public GamificationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ============ 用户游戏化数据 ============

    public Optional<UserGamification> findByUserId(Long userId) {
        var list = jdbc.query(
            "SELECT * FROM user_gamification WHERE user_id = ?",
            this::mapGamificationRow,
            userId
        );
        return list.stream().findFirst();
    }

    public void initUserGamification(Long userId) {
        jdbc.update(
            "INSERT IGNORE INTO user_gamification (user_id) VALUES (?)",
            userId
        );
    }

    public void updateGamification(Long userId, Integer expPoints, Integer level, String title,
                                    Integer streakDays, LocalDate lastPracticeDate,
                                    Integer totalInterviews, Integer totalQuestions, Integer bestScore) {
        jdbc.update("""
            UPDATE user_gamification SET
                exp_points = ?, level = ?, title = ?,
                streak_days = ?, last_practice_date = ?,
                total_interviews = ?, total_questions = ?, best_score = ?
            WHERE user_id = ?
            """,
            expPoints, level, title,
            streakDays, lastPracticeDate != null ? Date.valueOf(lastPracticeDate) : null,
            totalInterviews, totalQuestions, bestScore,
            userId
        );
    }

    public void addExp(Long userId, Integer exp) {
        jdbc.update(
            "UPDATE user_gamification SET exp_points = exp_points + ? WHERE user_id = ?",
            exp, userId
        );
    }

    // ============ 成就 ============

    public List<Achievement> findAllAchievements() {
        return jdbc.query(
            "SELECT * FROM achievements ORDER BY category, condition_value",
            this::mapAchievementRow
        );
    }

    public List<Achievement> findAchievementsByCategory(String category) {
        return jdbc.query(
            "SELECT * FROM achievements WHERE category = ? ORDER BY condition_value",
            this::mapAchievementRow,
            category
        );
    }

    public List<UserAchievement> findUserAchievements(Long userId) {
        return jdbc.query(
            "SELECT * FROM user_achievements WHERE user_id = ? ORDER BY unlocked_at DESC",
            this::mapUserAchievementRow,
            userId
        );
    }

    public boolean hasAchievement(Long userId, Long achievementId) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM user_achievements WHERE user_id = ? AND achievement_id = ?",
            Integer.class,
            userId, achievementId
        );
        return count != null && count > 0;
    }

    public void unlockAchievement(Long userId, Long achievementId) {
        jdbc.update(
            "INSERT IGNORE INTO user_achievements (user_id, achievement_id) VALUES (?, ?)",
            userId, achievementId
        );
    }

    // ============ 每日任务 ============

    public List<DailyTask> findDailyTasks(Long userId, LocalDate date) {
        return jdbc.query(
            "SELECT * FROM daily_tasks WHERE user_id = ? AND task_date = ? ORDER BY id",
            this::mapDailyTaskRow,
            userId, Date.valueOf(date)
        );
    }

    public void createDailyTask(DailyTask task) {
        jdbc.update("""
            INSERT IGNORE INTO daily_tasks (user_id, task_date, task_type, description, target_count, current_count, completed, claimed, exp_reward)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            task.userId(), Date.valueOf(task.taskDate()), task.taskType(), task.description(),
            task.targetCount(), task.currentCount(), task.completed(), task.claimed(), task.expReward()
        );
    }

    public void updateDailyTaskProgress(Long userId, LocalDate date, String taskType, Integer increment) {
        jdbc.update("""
            UPDATE daily_tasks SET
                current_count = LEAST(current_count + ?, target_count),
                completed = CASE WHEN current_count + ? >= target_count THEN 1 ELSE completed END
            WHERE user_id = ? AND task_date = ? AND task_type = ? AND claimed = 0
            """,
            increment, increment,
            userId, Date.valueOf(date), taskType
        );
    }

    public void claimDailyTask(Long userId, LocalDate date, String taskType) {
        jdbc.update(
            "UPDATE daily_tasks SET claimed = 1 WHERE user_id = ? AND task_date = ? AND task_type = ? AND completed = 1",
            userId, Date.valueOf(date), taskType
        );
    }

    // ============ Row Mappers ============

    private UserGamification mapGamificationRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new UserGamification(
            rs.getLong("user_id"),
            rs.getInt("exp_points"),
            rs.getInt("level"),
            rs.getString("title"),
            rs.getInt("streak_days"),
            rs.getDate("last_practice_date") != null ? rs.getDate("last_practice_date").toLocalDate() : null,
            rs.getInt("total_interviews"),
            rs.getInt("total_questions"),
            rs.getInt("best_score"),
            rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null,
            rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null
        );
    }

    private Achievement mapAchievementRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new Achievement(
            rs.getLong("id"),
            rs.getString("code"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getString("icon"),
            rs.getString("category"),
            rs.getString("condition_type"),
            rs.getInt("condition_value"),
            rs.getInt("exp_reward"),
            rs.getString("rarity"),
            rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null
        );
    }

    private UserAchievement mapUserAchievementRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new UserAchievement(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getLong("achievement_id"),
            rs.getTimestamp("unlocked_at") != null ? rs.getTimestamp("unlocked_at").toLocalDateTime() : null
        );
    }

    private DailyTask mapDailyTaskRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new DailyTask(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getDate("task_date").toLocalDate(),
            rs.getString("task_type"),
            rs.getString("description"),
            rs.getInt("target_count"),
            rs.getInt("current_count"),
            rs.getBoolean("completed"),
            rs.getBoolean("claimed"),
            rs.getInt("exp_reward")
        );
    }
}
