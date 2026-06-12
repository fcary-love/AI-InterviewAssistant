package com.faceai.pdfreader.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class KnowledgeRepository {

    private final JdbcTemplate jdbc;

    public KnowledgeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ============ 知识点 ============

    public List<Map<String, Object>> findAllByDirection(String direction) {
        return jdbc.queryForList(
                "SELECT * FROM knowledge_points WHERE direction = ? ORDER BY category, name",
                direction
        );
    }

    public Optional<Map<String, Object>> findById(Long id) {
        var list = jdbc.queryForList("SELECT * FROM knowledge_points WHERE id = ?", id);
        return list.stream().findFirst();
    }

    public List<Map<String, Object>> findByCategory(String direction, String category) {
        return jdbc.queryForList(
                "SELECT * FROM knowledge_points WHERE direction = ? AND category = ? ORDER BY name",
                direction, category
        );
    }

    // ============ 依赖关系 ============

    public List<Map<String, Object>> findDependencies(String direction) {
        return jdbc.queryForList("""
                SELECT d.*, p.name AS prerequisite_name, dp.name AS dependent_name
                FROM knowledge_dependencies d
                JOIN knowledge_points p ON d.prerequisite_id = p.id
                JOIN knowledge_points dp ON d.dependent_id = dp.id
                WHERE p.direction = ?
                """, direction
        );
    }

    public List<Map<String, Object>> findPrerequisites(Long knowledgePointId) {
        return jdbc.queryForList("""
                SELECT p.*, d.dependency_type
                FROM knowledge_dependencies d
                JOIN knowledge_points p ON d.prerequisite_id = p.id
                WHERE d.dependent_id = ?
                """, knowledgePointId
        );
    }

    public List<Map<String, Object>> findDependents(Long knowledgePointId) {
        return jdbc.queryForList("""
                SELECT p.*, d.dependency_type
                FROM knowledge_dependencies d
                JOIN knowledge_points p ON d.dependent_id = p.id
                WHERE d.prerequisite_id = ?
                """, knowledgePointId
        );
    }

    // ============ 题目-知识点关联 ============

    public List<Map<String, Object>> findKnowledgeByQuestionId(Long questionId) {
        return jdbc.queryForList("""
                SELECT kp.*, qkm.relevance_weight
                FROM question_knowledge_map qkm
                JOIN knowledge_points kp ON qkm.knowledge_point_id = kp.id
                WHERE qkm.question_id = ?
                """, questionId
        );
    }

    public List<Map<String, Object>> findQuestionsByKnowledgeId(Long knowledgePointId) {
        return jdbc.queryForList("""
                SELECT iq.*, qkm.relevance_weight
                FROM question_knowledge_map qkm
                JOIN interview_questions iq ON qkm.question_id = iq.id
                WHERE qkm.knowledge_point_id = ?
                """, knowledgePointId
        );
    }

    // ============ 用户掌握度 ============

    public Optional<Map<String, Object>> findMastery(Long userId, Long knowledgePointId) {
        var list = jdbc.queryForList(
                "SELECT * FROM user_knowledge_mastery WHERE user_id = ? AND knowledge_point_id = ?",
                userId, knowledgePointId
        );
        return list.stream().findFirst();
    }

    public List<Map<String, Object>> findAllMastery(Long userId) {
        return jdbc.queryForList(
                "SELECT * FROM user_knowledge_mastery WHERE user_id = ? ORDER BY mastery_level ASC",
                userId
        );
    }

    public List<Map<String, Object>> findWeakPoints(Long userId, int limit) {
        return jdbc.queryForList("""
                SELECT ukm.*, kp.name, kp.category, kp.direction
                FROM user_knowledge_mastery ukm
                JOIN knowledge_points kp ON ukm.knowledge_point_id = kp.id
                WHERE ukm.user_id = ?
                ORDER BY ukm.mastery_level ASC
                LIMIT ?
                """, userId, limit
        );
    }

    public void upsertMastery(Long userId, Long knowledgePointId, double newMastery, boolean isCorrect) {
        jdbc.update("""
                INSERT INTO user_knowledge_mastery (user_id, knowledge_point_id, mastery_level, attempt_count, correct_count, last_attempt_at)
                VALUES (?, ?, ?, 1, ?, NOW())
                ON DUPLICATE KEY UPDATE
                    mastery_level = VALUES(mastery_level),
                    attempt_count = attempt_count + 1,
                    correct_count = correct_count + VALUES(correct_count),
                    last_attempt_at = NOW()
                """,
                userId, knowledgePointId, newMastery, isCorrect ? 1 : 0
        );
    }

    // ============ 统计 ============

    public int countByDirection(String direction) {
        var list = jdbc.queryForList(
                "SELECT COUNT(*) AS cnt FROM knowledge_points WHERE direction = ?",
                direction
        );
        return list.isEmpty() ? 0 : ((Number) list.get(0).get("cnt")).intValue();
    }

    public int countDependencies(Long knowledgePointId) {
        var list = jdbc.queryForList(
                "SELECT COUNT(*) AS cnt FROM knowledge_dependencies WHERE prerequisite_id = ?",
                knowledgePointId
        );
        return list.isEmpty() ? 0 : ((Number) list.get(0).get("cnt")).intValue();
    }
}
