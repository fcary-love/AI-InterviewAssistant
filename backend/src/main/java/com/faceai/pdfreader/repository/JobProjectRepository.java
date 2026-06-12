package com.faceai.pdfreader.repository;

import com.faceai.pdfreader.model.JobProjectDetailResponse;
import com.faceai.pdfreader.model.JobProjectSummaryResponse;
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
public class JobProjectRepository {

    private final JdbcTemplate jdbcTemplate;

    public JobProjectRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public JobProjectDetailResponse create(
            Long userId,
            String companyName,
            String jobTitle,
            String jdText,
            Long resumeVersionId,
            String resumeFileId
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                            INSERT INTO job_projects
                                (user_id, company_name, job_title, jd_text, resume_version_id, resume_file_id, status)
                            VALUES (?, ?, ?, ?, ?, ?, '待分析')
                            """,
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, userId);
            statement.setString(2, companyName);
            statement.setString(3, jobTitle);
            statement.setString(4, jdText);
            statement.setObject(5, resumeVersionId);
            statement.setString(6, resumeFileId);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return findDetail(userId, key == null ? null : key.longValue())
                .orElseThrow(() -> new IllegalArgumentException("岗位项目创建失败"));
    }

    public List<JobProjectSummaryResponse> list(Long userId, int limit) {
        return jdbcTemplate.query("""
                        SELECT jp.id, jp.company_name, jp.job_title, jp.status, jp.match_score,
                               COALESCE(rv.file_name, df.file_name, '') AS resume_file_name,
                               jp.jd_text, jp.updated_at
                        FROM job_projects jp
                        LEFT JOIN resume_versions rv ON rv.id = jp.resume_version_id AND rv.user_id = jp.user_id
                        LEFT JOIN document_files df ON df.file_id = jp.resume_file_id AND df.user_id = jp.user_id
                        WHERE jp.user_id = ?
                        ORDER BY jp.updated_at DESC, jp.id DESC
                        LIMIT ?
                        """,
                (rs, rowNum) -> new JobProjectSummaryResponse(
                        rs.getLong("id"),
                        rs.getString("company_name"),
                        rs.getString("job_title"),
                        rs.getString("status"),
                        rs.getObject("match_score", Integer.class),
                        rs.getString("resume_file_name"),
                        excerpt(rs.getString("jd_text"), 90),
                        formatTimestamp(rs.getTimestamp("updated_at"))
                ),
                userId,
                Math.max(1, Math.min(limit, 100))
        );
    }

    public Optional<JobProjectDetailResponse> findDetail(Long userId, Long id) {
        return jdbcTemplate.query("""
                        SELECT jp.id, jp.company_name, jp.job_title, jp.jd_text, jp.resume_version_id,
                               jp.resume_file_id, jp.match_analysis_id, jp.match_score, jp.status,
                               jp.resume_suggestions, jp.tailored_resume_text, jp.final_conclusion,
                               jp.created_at, jp.updated_at,
                               COALESCE(rv.file_name, df.file_name, '') AS resume_file_name,
                               rv.version_no
                        FROM job_projects jp
                        LEFT JOIN resume_versions rv ON rv.id = jp.resume_version_id AND rv.user_id = jp.user_id
                        LEFT JOIN document_files df ON df.file_id = jp.resume_file_id AND df.user_id = jp.user_id
                        WHERE jp.user_id = ? AND jp.id = ?
                        """,
                (rs, rowNum) -> new JobProjectDetailResponse(
                        rs.getLong("id"),
                        rs.getString("company_name"),
                        rs.getString("job_title"),
                        rs.getString("jd_text"),
                        rs.getObject("resume_version_id", Long.class),
                        rs.getString("resume_file_id"),
                        rs.getString("resume_file_name"),
                        rs.getObject("version_no", Integer.class),
                        rs.getObject("match_analysis_id", Long.class),
                        rs.getObject("match_score", Integer.class),
                        rs.getString("status"),
                        rs.getString("resume_suggestions"),
                        rs.getString("tailored_resume_text"),
                        rs.getString("final_conclusion"),
                        formatTimestamp(rs.getTimestamp("created_at")),
                        formatTimestamp(rs.getTimestamp("updated_at")),
                        List.of(),
                        List.of(),
                        List.of()
                ),
                userId,
                id
        ).stream().findFirst();
    }

    public void updateMatch(
            Long userId,
            Long id,
            Long matchAnalysisId,
            Integer matchScore,
            String resumeSuggestions
    ) {
        jdbcTemplate.update("""
                        UPDATE job_projects
                        SET match_analysis_id = ?,
                            match_score = ?,
                            resume_suggestions = ?,
                            status = '已匹配'
                        WHERE user_id = ? AND id = ?
                        """,
                matchAnalysisId,
                matchScore,
                resumeSuggestions,
                userId,
                id
        );
    }

    public void updateTailoredResume(Long userId, Long id, String tailoredResumeText) {
        jdbcTemplate.update("""
                        UPDATE job_projects
                        SET tailored_resume_text = ?,
                            status = '已优化'
                        WHERE user_id = ? AND id = ?
                        """,
                tailoredResumeText,
                userId,
                id
        );
    }

    public void updateStatus(Long userId, Long id, String status) {
        jdbcTemplate.update("""
                        UPDATE job_projects
                        SET status = ?
                        WHERE user_id = ? AND id = ?
                        """,
                status,
                userId,
                id
        );
    }

    public void updateConclusion(Long userId, Long id, String finalConclusion) {
        jdbcTemplate.update("""
                        UPDATE job_projects
                        SET final_conclusion = ?
                        WHERE user_id = ? AND id = ?
                        """,
                finalConclusion,
                userId,
                id
        );
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
