package com.faceai.pdfreader.repository;

import com.faceai.pdfreader.model.ScoreTrendPointResponse;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class GrowthDashboardRepository {

    private final JdbcTemplate jdbcTemplate;

    public GrowthDashboardRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int countResumeVersions(Long userId) {
        return queryInt("SELECT COUNT(*) FROM resume_versions WHERE user_id = ?", userId);
    }

    public int countJobMatches(Long userId) {
        return queryInt("SELECT COUNT(*) FROM job_match_analyses WHERE user_id = ?", userId);
    }

    public int averageMatchScore(Long userId) {
        return queryInt("""
                SELECT ROUND(AVG(match_score))
                FROM job_match_analyses
                WHERE user_id = ? AND match_score IS NOT NULL
                """, userId);
    }

    public int countInterviewSessions(Long userId) {
        return queryInt("SELECT COUNT(*) FROM interview_sessions WHERE user_id = ?", userId);
    }

    public int countInterviewReports(Long userId) {
        return queryInt("SELECT COUNT(*) FROM interview_reports WHERE user_id = ?", userId);
    }

    public int averageInterviewScore(Long userId) {
        return queryInt("""
                SELECT ROUND(AVG(total_score))
                FROM interview_reports
                WHERE user_id = ? AND total_score IS NOT NULL
                """, userId);
    }

    public List<ScoreTrendPointResponse> scoreTrend(Long userId) {
        return jdbcTemplate.query("""
                        SELECT session_id, total_score, updated_at
                        FROM interview_reports
                        WHERE user_id = ? AND total_score IS NOT NULL
                        ORDER BY updated_at ASC
                        LIMIT 12
                        """,
                (rs, rowNum) -> new ScoreTrendPointResponse(
                        "第 " + (rowNum + 1) + " 次",
                        rs.getObject("total_score", Integer.class),
                        formatTimestamp(rs.getTimestamp("updated_at"))
                ),
                userId
        );
    }

    public List<String> lowScoreTexts(Long userId) {
        return jdbcTemplate.query("""
                        SELECT CONCAT(COALESCE(t.question, ''), ' ', COALESCE(t.ai_comment, '')) AS content
                        FROM interview_turns t
                        INNER JOIN interview_sessions s ON s.session_id = t.session_id
                        WHERE s.user_id = ? AND (t.score IS NULL OR t.score < 70)
                        ORDER BY t.created_at DESC
                        LIMIT 80
                        """,
                (rs, rowNum) -> rs.getString("content"),
                userId
        );
    }

    public String latestReportSummary(Long userId) {
        return jdbcTemplate.query("""
                        SELECT COALESCE(s.summary, '') AS summary
                        FROM interview_reports r
                        LEFT JOIN interview_sessions s ON s.session_id = r.session_id AND s.user_id = r.user_id
                        WHERE r.user_id = ?
                        ORDER BY r.updated_at DESC
                        LIMIT 1
                        """,
                (rs, rowNum) -> rs.getString("summary"),
                userId
        ).stream().findFirst().orElse("");
    }

    private int queryInt(String sql, Object... args) {
        Number value = jdbcTemplate.queryForObject(sql, Number.class, args);
        return value == null ? 0 : value.intValue();
    }

    private String formatTimestamp(Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toLocalDateTime().toString().replace("T", " ");
    }
}
