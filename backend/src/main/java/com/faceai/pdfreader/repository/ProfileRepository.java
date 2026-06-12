package com.faceai.pdfreader.repository;

import com.faceai.pdfreader.model.JobMatchHistoryResponse;
import com.faceai.pdfreader.model.ResumeVersionDetailResponse;
import com.faceai.pdfreader.model.ResumeVersionResponse;
import com.faceai.pdfreader.model.UserProfileResponse;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProfileRepository {

    private final JdbcTemplate jdbcTemplate;

    public ProfileRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UserProfileResponse findProfile(Long userId) {
        return jdbcTemplate.query("""
                        SELECT id, display_name, target_role, skill_keywords, created_at
                        FROM user_profiles
                        WHERE id = ?
                        """,
                (rs, rowNum) -> new UserProfileResponse(
                        rs.getLong("id"),
                        rs.getString("display_name"),
                        rs.getString("target_role"),
                        rs.getString("skill_keywords"),
                        formatTimestamp(rs.getTimestamp("created_at"))
                ),
                userId
        ).stream().findFirst().orElseGet(() -> new UserProfileResponse(
                userId,
                "本地用户",
                "Java 后端开发",
                "Java, Spring Boot, MySQL, Redis, Vue3, Docker",
                ""
        ));
    }

    public void saveResumeVersion(
            Long userId,
            String fileId,
            String fileName,
            String fileType,
            int textLength,
            String skillKeywords,
            String contentPreview
    ) {
        Integer nextVersion = jdbcTemplate.queryForObject("""
                        SELECT COALESCE(MAX(version_no), 0) + 1
                        FROM resume_versions
                        WHERE user_id = ?
                        """,
                Integer.class,
                userId
        );

        jdbcTemplate.update("""
                        INSERT INTO resume_versions
                            (user_id, file_id, file_name, file_type, version_no, text_length, skill_keywords, content_preview)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            file_name = VALUES(file_name),
                            file_type = VALUES(file_type),
                            text_length = VALUES(text_length),
                            skill_keywords = VALUES(skill_keywords),
                            content_preview = VALUES(content_preview)
                        """,
                userId,
                fileId,
                fileName,
                fileType,
                nextVersion == null ? 1 : nextVersion,
                textLength,
                skillKeywords,
                contentPreview
        );
    }

    public List<ResumeVersionResponse> listRecentResumeVersions(Long userId, int limit) {
        return jdbcTemplate.query("""
                        SELECT id, file_id, file_name, file_type, version_no, text_length,
                               skill_keywords, content_preview, created_at
                        FROM resume_versions
                        WHERE user_id = ?
                        ORDER BY created_at DESC, id DESC
                        LIMIT ?
                        """,
                (rs, rowNum) -> new ResumeVersionResponse(
                        rs.getLong("id"),
                        rs.getString("file_id"),
                        rs.getString("file_name"),
                        rs.getString("file_type"),
                        rs.getInt("version_no"),
                        rs.getInt("text_length"),
                        rs.getString("skill_keywords"),
                        rs.getString("content_preview"),
                        formatTimestamp(rs.getTimestamp("created_at"))
                ),
                userId,
                limit
        );
    }

    public List<ResumeVersionResponse> listResumeVersions(Long userId, int limit) {
        return listRecentResumeVersions(userId, limit);
    }

    public Optional<ResumeVersionDetailResponse> findResumeVersionDetail(Long userId, Long id) {
        return jdbcTemplate.query("""
                        SELECT rv.id, rv.file_id, rv.file_name, rv.file_type, rv.version_no,
                               rv.text_length, rv.skill_keywords, rv.content_preview,
                               df.full_text, rv.created_at
                        FROM resume_versions rv
                        LEFT JOIN document_files df ON df.file_id = rv.file_id AND df.user_id = rv.user_id
                        WHERE rv.id = ? AND rv.user_id = ?
                        """,
                (rs, rowNum) -> new ResumeVersionDetailResponse(
                        rs.getLong("id"),
                        rs.getString("file_id"),
                        rs.getString("file_name"),
                        rs.getString("file_type"),
                        rs.getInt("version_no"),
                        rs.getInt("text_length"),
                        rs.getString("skill_keywords"),
                        rs.getString("content_preview"),
                        rs.getString("full_text"),
                        formatTimestamp(rs.getTimestamp("created_at"))
                ),
                id,
                userId
        ).stream().findFirst();
    }

    public int countResumeVersions(Long userId) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM resume_versions
                        WHERE user_id = ?
                        """,
                Integer.class,
                userId
        );
        return count == null ? 0 : count;
    }

    public List<JobMatchHistoryResponse> listRecentJobMatches(Long userId, int limit) {
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
                        excerpt(rs.getString("jd_text"), 80),
                        formatTimestamp(rs.getTimestamp("created_at"))
                ),
                userId,
                limit
        );
    }

    public int countJobMatches(Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM job_match_analyses WHERE user_id = ?",
                Integer.class,
                userId
        );
        return count == null ? 0 : count;
    }

    public int averageMatchScore(Long userId) {
        Number average = jdbcTemplate.queryForObject("""
                        SELECT ROUND(AVG(match_score))
                        FROM job_match_analyses
                        WHERE user_id = ? AND match_score IS NOT NULL
                        """,
                Number.class,
                userId
        );
        return average == null ? 0 : average.intValue();
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

    private String formatTimestamp(Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toLocalDateTime().toString().replace("T", " ");
    }
}
