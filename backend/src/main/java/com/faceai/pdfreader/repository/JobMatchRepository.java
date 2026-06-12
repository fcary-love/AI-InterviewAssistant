package com.faceai.pdfreader.repository;

import com.faceai.pdfreader.model.JobMatchDetailResponse;
import com.faceai.pdfreader.model.JobMatchHistoryResponse;
import com.faceai.pdfreader.model.JobMatchResponse;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;

@Repository
public class JobMatchRepository {

    private final JdbcTemplate jdbcTemplate;

    public JobMatchRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public JobMatchResponse save(Long userId, String resumeFileId, String jdText, Integer matchScore, String analysisContent) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                            INSERT INTO job_match_analyses (user_id, resume_file_id, jd_text, match_score, analysis_content)
                            VALUES (?, ?, ?, ?, ?)
                            """,
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, userId);
            statement.setString(2, resumeFileId);
            statement.setString(3, jdText);
            statement.setObject(4, matchScore);
            statement.setString(5, analysisContent);
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        Long id = key == null ? null : key.longValue();
        return findById(userId, id);
    }

    public List<JobMatchHistoryResponse> list(Long userId, int limit) {
        return jdbcTemplate.query("""
                        SELECT id, resume_file_id, match_score, jd_text, created_at
                        FROM job_match_analyses
                        WHERE user_id = ?
                        ORDER BY created_at DESC, id DESC
                        LIMIT ?
                        """,
                (rs, rowNum) -> new JobMatchHistoryResponse(
                        rs.getLong("id"),
                        rs.getString("resume_file_id"),
                        rs.getObject("match_score", Integer.class),
                        excerpt(rs.getString("jd_text"), 96),
                        formatTimestamp(rs.getTimestamp("created_at"))
                ),
                userId,
                Math.max(1, Math.min(limit, 100))
        );
    }

    public Optional<JobMatchDetailResponse> findDetail(Long userId, Long id) {
        return jdbcTemplate.query("""
                        SELECT jm.id, jm.resume_file_id, COALESCE(df.file_name, rv.file_name, '') AS resume_file_name,
                               jm.match_score, jm.jd_text, jm.analysis_content, jm.created_at
                        FROM job_match_analyses jm
                        LEFT JOIN document_files df ON df.file_id = jm.resume_file_id AND df.user_id = jm.user_id
                        LEFT JOIN resume_versions rv ON rv.file_id = jm.resume_file_id AND rv.user_id = jm.user_id
                        WHERE jm.user_id = ? AND jm.id = ?
                        """,
                (rs, rowNum) -> new JobMatchDetailResponse(
                        rs.getLong("id"),
                        rs.getString("resume_file_id"),
                        rs.getString("resume_file_name"),
                        rs.getObject("match_score", Integer.class),
                        rs.getString("jd_text"),
                        rs.getString("analysis_content"),
                        formatTimestamp(rs.getTimestamp("created_at")),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()
                ),
                userId,
                id
        ).stream().findFirst();
    }

    private JobMatchResponse findById(Long userId, Long id) {
        return jdbcTemplate.query("""
                        SELECT id, resume_file_id, match_score, analysis_content, created_at
                        FROM job_match_analyses
                        WHERE user_id = ? AND id = ?
                        """,
                (rs, rowNum) -> new JobMatchResponse(
                        rs.getLong("id"),
                        rs.getString("resume_file_id"),
                        rs.getObject("match_score", Integer.class),
                        rs.getString("analysis_content"),
                        formatTimestamp(rs.getTimestamp("created_at"))
                ),
                userId,
                id
        ).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("岗位匹配分析保存失败"));
    }

    private String formatTimestamp(Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toLocalDateTime().toString().replace("T", " ");
    }

    private String excerpt(String text, int maxLength) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String clean = text.replaceAll("\\s+", " ").trim();
        if (clean.length() <= maxLength) {
            return clean;
        }
        return clean.substring(0, maxLength) + "...";
    }
}
