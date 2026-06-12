package com.faceai.pdfreader.repository;

import com.faceai.pdfreader.service.EloRatingService.DifficultyTrajectory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class EloRepository {

    private final JdbcTemplate jdbc;

    public EloRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ============ 用户 Elo 评分 ============

    public Optional<Double> findUserRating(Long userId, String direction) {
        var list = jdbc.queryForList(
                "SELECT elo_rating FROM user_elo_ratings WHERE user_id = ? AND direction = ? AND category = ''",
                userId, direction
        );
        return list.stream()
                .map(row -> ((Number) row.get("elo_rating")).doubleValue())
                .findFirst();
    }

    public int findUserGamesPlayed(Long userId, String direction) {
        var list = jdbc.queryForList(
                "SELECT games_played FROM user_elo_ratings WHERE user_id = ? AND direction = ? AND category = ''",
                userId, direction
        );
        return list.stream()
                .map(row -> ((Number) row.get("games_played")).intValue())
                .findFirst()
                .orElse(0);
    }

    public void upsertUserRating(Long userId, String direction, double newElo) {
        jdbc.update("""
                INSERT INTO user_elo_ratings (user_id, direction, category, elo_rating, games_played)
                VALUES (?, ?, '', ?, 1)
                ON DUPLICATE KEY UPDATE
                    elo_rating = VALUES(elo_rating),
                    games_played = games_played + 1
                """,
                userId, direction, newElo
        );
    }

    // ============ 题目 Elo 难度 ============

    public Optional<Double> findQuestionDifficulty(Long questionId) {
        var list = jdbc.queryForList(
                "SELECT elo_difficulty FROM question_difficulty_ratings WHERE question_id = ?",
                questionId
        );
        return list.stream()
                .map(row -> ((Number) row.get("elo_difficulty")).doubleValue())
                .findFirst();
    }

    public void upsertQuestionDifficulty(Long questionId, double newElo, int score) {
        jdbc.update("""
                INSERT INTO question_difficulty_ratings (question_id, elo_difficulty, attempt_count, avg_score)
                VALUES (?, ?, 1, ?)
                ON DUPLICATE KEY UPDATE
                    elo_difficulty = VALUES(elo_difficulty),
                    attempt_count = attempt_count + 1,
                    avg_score = (avg_score * attempt_count + VALUES(avg_score)) / (attempt_count + 1)
                """,
                questionId, newElo, score
        );
    }

    // ============ 难度变化轨迹 ============

    public void saveTrajectory(Long userId, String sessionId, int questionNo,
                                double userEloBefore, double questionElo, int score,
                                double userEloAfter, String difficultyLabel) {
        jdbc.update("""
                INSERT INTO difficulty_trajectories
                    (user_id, session_id, question_no, user_elo_before, question_elo, score, user_elo_after, difficulty_label)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                userId, sessionId, questionNo,
                userEloBefore, questionElo, score, userEloAfter, difficultyLabel
        );
    }

    public List<DifficultyTrajectory> findTrajectories(Long userId, String sessionId) {
        return jdbc.query(
                "SELECT * FROM difficulty_trajectories WHERE user_id = ? AND session_id = ? ORDER BY question_no",
                (rs, rowNum) -> new DifficultyTrajectory(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        rs.getString("session_id"),
                        rs.getInt("question_no"),
                        rs.getDouble("user_elo_before"),
                        rs.getDouble("question_elo"),
                        rs.getInt("score"),
                        rs.getDouble("user_elo_after"),
                        rs.getString("difficulty_label"),
                        rs.getString("created_at")
                ),
                userId, sessionId
        );
    }
}
