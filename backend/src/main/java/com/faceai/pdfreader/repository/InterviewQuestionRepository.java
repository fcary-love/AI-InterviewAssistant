package com.faceai.pdfreader.repository;

import com.faceai.pdfreader.model.InterviewQuestionRecord;
import com.faceai.pdfreader.model.QuestionBankItemResponse;
import com.faceai.pdfreader.model.QuestionBankOverviewResponse;
import com.faceai.pdfreader.model.QuestionFacetResponse;
import com.faceai.pdfreader.model.WrongQuestionResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class InterviewQuestionRepository {

    private final JdbcTemplate jdbcTemplate;

    public InterviewQuestionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<InterviewQuestionRecord> findOpeningQuestion(
            String direction,
            String focus,
            String difficulty
    ) {
        return jdbcTemplate.query("""
                        SELECT id, direction, category, difficulty, question_text, reference_answer, source_name, source_url
                        FROM interview_questions
                        WHERE enabled = TRUE
                          AND (direction = ? OR direction = '软件开发')
                        ORDER BY
                          CASE
                            WHEN category = ? THEN 0
                            WHEN category LIKE ? THEN 1
                            ELSE 2
                          END,
                          CASE WHEN difficulty = ? THEN 0 ELSE 1 END,
                          RAND()
                        LIMIT 1
                        """,
                (rs, rowNum) -> mapRow(rs),
                normalizeDirection(direction),
                normalizeFocus(focus),
                "%" + normalizeFocus(focus) + "%",
                normalizeDifficulty(difficulty)
        ).stream().findFirst();
    }

    public Optional<InterviewQuestionRecord> findNextQuestion(
            String sessionId,
            String direction,
            String focus,
            String difficulty
    ) {
        return jdbcTemplate.query("""
                        SELECT q.id, q.direction, q.category, q.difficulty, q.question_text, q.reference_answer, q.source_name, q.source_url
                        FROM interview_questions q
                        WHERE q.enabled = TRUE
                          AND (q.direction = ? OR q.direction = '软件开发')
                          AND NOT EXISTS (
                              SELECT 1
                              FROM interview_turns t
                              WHERE t.session_id = ? AND t.question = q.question_text
                          )
                        ORDER BY
                          CASE
                            WHEN q.category = ? THEN 0
                            WHEN q.category LIKE ? THEN 1
                            ELSE 2
                          END,
                          CASE WHEN q.difficulty = ? THEN 0 ELSE 1 END,
                          RAND()
                        LIMIT 1
                        """,
                (rs, rowNum) -> mapRow(rs),
                normalizeDirection(direction),
                sessionId,
                normalizeFocus(focus),
                "%" + normalizeFocus(focus) + "%",
                normalizeDifficulty(difficulty)
        ).stream().findFirst();
    }

    public QuestionBankOverviewResponse overview() {
        Integer total = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM interview_questions
                        WHERE enabled = TRUE
                        """,
                Integer.class
        );
        return new QuestionBankOverviewResponse(
                total == null ? 0 : total,
                listFacets("direction"),
                listFacets("category"),
                listFacets("difficulty")
        );
    }

    public List<QuestionBankItemResponse> listQuestions(
            String direction,
            String category,
            String difficulty,
            String keyword,
            Integer limit
    ) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, direction, category, difficulty, question_text, reference_answer, source_name, source_url
                FROM interview_questions
                WHERE enabled = TRUE
                """);
        List<Object> params = new ArrayList<>();
        if (StringUtils.hasText(direction)) {
            sql.append(" AND direction = ?");
            params.add(direction);
        }
        if (StringUtils.hasText(category)) {
            sql.append(" AND category = ?");
            params.add(category);
        }
        if (StringUtils.hasText(difficulty)) {
            sql.append(" AND difficulty = ?");
            params.add(difficulty);
        }
        if (StringUtils.hasText(keyword)) {
            sql.append(" AND (question_text LIKE ? OR reference_answer LIKE ? OR category LIKE ?)");
            String likeKeyword = "%" + keyword.trim() + "%";
            params.add(likeKeyword);
            params.add(likeKeyword);
            params.add(likeKeyword);
        }
        sql.append(" ORDER BY direction, category, FIELD(difficulty, '简单', '标准', '困难'), id LIMIT ?");
        params.add(normalizeLimit(limit));
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> mapQuestionBankRow(rs), params.toArray());
    }

    public List<WrongQuestionResponse> listWrongQuestions(Long userId, Integer maxScore, Integer limit) {
        return jdbcTemplate.query("""
                        SELECT t.session_id, t.question_no, t.question, t.answer, t.score, t.ai_comment,
                               t.duration_seconds, t.created_at, t.reviewed
                        FROM interview_turns t
                        INNER JOIN interview_sessions s ON s.session_id = t.session_id
                        WHERE s.user_id = ? AND t.score IS NOT NULL AND t.score < ?
                        ORDER BY t.reviewed ASC, t.created_at DESC
                        LIMIT ?
                        """,
                (rs, rowNum) -> new WrongQuestionResponse(
                        rs.getString("session_id"),
                        rs.getInt("question_no"),
                        rs.getString("question"),
                        rs.getString("answer"),
                        rs.getObject("score", Integer.class),
                        rs.getString("ai_comment"),
                        rs.getObject("duration_seconds", Integer.class),
                        formatTimestamp(rs.getTimestamp("created_at")),
                        rs.getBoolean("reviewed")
                ),
                userId,
                maxScore == null ? 70 : Math.max(1, Math.min(maxScore, 100)),
                normalizeLimit(limit)
        );
    }

    public void markReviewed(String sessionId, Integer questionNo) {
        jdbcTemplate.update(
                "UPDATE interview_turns SET reviewed = TRUE WHERE session_id = ? AND question_no = ?",
                sessionId, questionNo
        );
    }

    public List<InterviewQuestionRecord> findAll() {
        return jdbcTemplate.query(
                "SELECT id, direction, category, difficulty, question_text, reference_answer, source_name, source_url FROM interview_questions WHERE enabled = TRUE",
                (rs, rowNum) -> mapRow(rs)
        );
    }

    private List<QuestionFacetResponse> listFacets(String columnName) {
        if (!List.of("direction", "category", "difficulty").contains(columnName)) {
            throw new IllegalArgumentException("不支持的题库维度");
        }
        return jdbcTemplate.query("""
                        SELECT %s AS name, COUNT(*) AS total
                        FROM interview_questions
                        WHERE enabled = TRUE
                        GROUP BY %s
                        ORDER BY total DESC, %s ASC
                        """.formatted(columnName, columnName, columnName),
                (rs, rowNum) -> new QuestionFacetResponse(
                        rs.getString("name"),
                        rs.getInt("total")
                )
        );
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return 60;
        }
        return Math.max(1, Math.min(limit, 200));
    }

    private String normalizeDirection(String direction) {
        if (!StringUtils.hasText(direction)) {
            return "软件开发";
        }
        if (direction.contains("后端")) {
            return "后端开发";
        }
        if (direction.contains("前端")) {
            return "前端开发";
        }
        return direction.contains("软件") ? "软件开发" : direction;
    }

    private String normalizeFocus(String focus) {
        if (!StringUtils.hasText(focus)) {
            return "项目深挖";
        }
        if ("basic".equalsIgnoreCase(focus)) {
            return "Java 基础";
        }
        if ("project".equalsIgnoreCase(focus)) {
            return "项目深挖";
        }
        if ("system".equalsIgnoreCase(focus)) {
            return "系统设计";
        }
        if ("mixed".equalsIgnoreCase(focus)) {
            return "综合能力";
        }
        if (focus.contains("项目")) {
            return "项目深挖";
        }
        if (focus.contains("基础") || focus.contains("八股")) {
            return "Java 基础";
        }
        return focus;
    }

    private String normalizeDifficulty(String difficulty) {
        if (!StringUtils.hasText(difficulty) || difficulty.contains("随机")) {
            return "标准";
        }
        return difficulty;
    }

    private InterviewQuestionRecord mapRow(ResultSet rs) throws SQLException {
        return new InterviewQuestionRecord(
                rs.getLong("id"),
                rs.getString("direction"),
                rs.getString("category"),
                rs.getString("difficulty"),
                rs.getString("question_text"),
                rs.getString("reference_answer"),
                rs.getString("source_name"),
                rs.getString("source_url")
        );
    }

    private QuestionBankItemResponse mapQuestionBankRow(ResultSet rs) throws SQLException {
        return new QuestionBankItemResponse(
                rs.getLong("id"),
                rs.getString("direction"),
                rs.getString("category"),
                rs.getString("difficulty"),
                rs.getString("question_text"),
                rs.getString("reference_answer"),
                rs.getString("source_name"),
                rs.getString("source_url")
        );
    }

    private String formatTimestamp(Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toLocalDateTime().toString().replace("T", " ");
    }
}
