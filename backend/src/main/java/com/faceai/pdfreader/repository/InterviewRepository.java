package com.faceai.pdfreader.repository;

import com.faceai.pdfreader.model.InterviewSessionResponse;
import com.faceai.pdfreader.model.InterviewStartRequest;
import com.faceai.pdfreader.model.InterviewReportResponse;
import com.faceai.pdfreader.model.InterviewReportSummaryResponse;
import com.faceai.pdfreader.model.InterviewTurnRecord;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class InterviewRepository {

    private final JdbcTemplate jdbcTemplate;

    public InterviewRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(Long userId, String sessionId, InterviewStartRequest request, InterviewSessionResponse response) {
        jdbcTemplate.update("""
                        INSERT INTO interview_sessions (
                            user_id, session_id, resume_file_id, jd_file_id, jd_text, resume_text,
                            candidate_profile, direction, difficulty, focus_area,
                            interviewer_style, question_mode, random_mix, status, summary, first_question
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                userId,
                sessionId,
                request.resumeFileId(),
                request.jdFileId(),
                request.jdText(),
                truncate(response.resumeText(), 10000),
                response.candidateProfile(),
                request.direction(),
                request.difficulty(),
                request.focus(),
                request.style(),
                request.questionMode(),
                Boolean.TRUE.equals(request.randomMix()),
                response.status(),
                response.summary(),
                response.firstQuestion()
        );
    }

    public Optional<InterviewSessionResponse> findBySessionId(Long userId, String sessionId) {
        return jdbcTemplate.query("""
                        SELECT session_id, status, summary, first_question, resume_text, jd_text, candidate_profile
                        FROM interview_sessions
                        WHERE user_id = ? AND session_id = ?
                        """,
                (rs, rowNum) -> mapRow(rs),
                userId,
                sessionId
        ).stream().findFirst();
    }

    public int countTurns(String sessionId) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM interview_turns
                        WHERE session_id = ?
                        """,
                Integer.class,
                sessionId
        );
        return count == null ? 0 : count;
    }

    public void saveTurn(InterviewTurnRecord turn) {
        jdbcTemplate.update("""
                        INSERT INTO interview_turns (
                            session_id, question_no, question, answer, duration_seconds,
                            ai_comment, score, scores, follow_up
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                turn.sessionId(),
                turn.questionNo(),
                turn.question(),
                turn.answer(),
                turn.durationSeconds(),
                turn.aiComment(),
                turn.score(),
                turn.scores(),
                Boolean.TRUE.equals(turn.followUp())
        );
    }

    public void updateTurnEvaluation(String sessionId, Integer questionNo, Integer score, String aiComment, String scores) {
        jdbcTemplate.update("""
                        UPDATE interview_turns
                        SET score = ?, ai_comment = ?, scores = ?
                        WHERE session_id = ? AND question_no = ?
                        """,
                score,
                aiComment,
                scores,
                sessionId,
                questionNo
        );
    }

    public void updateTurnEvaluation(String sessionId, Integer questionNo, Integer score, String aiComment) {
        updateTurnEvaluation(sessionId, questionNo, score, aiComment, null);
    }

    public List<InterviewTurnRecord> findTurnsBySessionId(String sessionId) {
        return jdbcTemplate.query("""
                        SELECT session_id, question_no, question, answer, duration_seconds,
                               ai_comment, score, scores, follow_up
                        FROM interview_turns
                        WHERE session_id = ?
                        ORDER BY question_no ASC
                        """,
                (rs, rowNum) -> mapTurnRow(rs),
                sessionId
        );
    }

    public void markFinished(String sessionId) {
        jdbcTemplate.update("""
                        UPDATE interview_sessions
                        SET status = 'FINISHED'
                        WHERE session_id = ?
                        """,
                sessionId
        );
    }

    public void saveReport(Long userId, InterviewReportResponse report) {
        jdbcTemplate.update("""
                        INSERT INTO interview_reports (user_id, session_id, total_score, report_content,
                                                       user_reflection, scores, strengths, weaknesses, advice)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            user_id = VALUES(user_id),
                            total_score = VALUES(total_score),
                            report_content = VALUES(report_content),
                            user_reflection = VALUES(user_reflection),
                            scores = VALUES(scores),
                            strengths = VALUES(strengths),
                            weaknesses = VALUES(weaknesses),
                            advice = VALUES(advice),
                            updated_at = CURRENT_TIMESTAMP
                        """,
                userId,
                report.sessionId(),
                report.totalScore(),
                report.reportContent(),
                report.userReflection(),
                report.scores(),
                report.strengths(),
                report.weaknesses(),
                report.advice()
        );
    }

    public Optional<InterviewReportResponse> findReportBySessionId(Long userId, String sessionId) {
        return jdbcTemplate.query("""
                        SELECT r.session_id, r.total_score, r.report_content, r.user_reflection, r.updated_at,
                               r.scores, r.strengths, r.weaknesses, r.advice,
                               COALESCE(s.summary, '') AS summary
                        FROM interview_reports r
                        LEFT JOIN interview_sessions s ON s.session_id = r.session_id AND s.user_id = r.user_id
                        WHERE r.user_id = ? AND r.session_id = ?
                        """,
                (rs, rowNum) -> mapReportRow(rs),
                userId,
                sessionId
        ).stream().findFirst();
    }

    public List<InterviewReportSummaryResponse> findReportSummaries(Long userId) {
        return jdbcTemplate.query("""
                        SELECT r.session_id, r.total_score, r.updated_at, r.scores,
                               COALESCE(s.summary, '') AS summary
                        FROM interview_reports r
                        LEFT JOIN interview_sessions s ON s.session_id = r.session_id AND s.user_id = r.user_id
                        WHERE r.user_id = ?
                        ORDER BY r.updated_at DESC
                        """,
                (rs, rowNum) -> new InterviewReportSummaryResponse(
                        rs.getString("session_id"),
                        rs.getObject("total_score", Integer.class),
                        rs.getString("summary"),
                        formatTimestamp(rs.getTimestamp("updated_at")),
                        rs.getString("scores")
                ),
                userId
        );
    }

    public boolean deleteReport(Long userId, String sessionId) {
        int affectedRows = jdbcTemplate.update("""
                        DELETE FROM interview_reports
                        WHERE user_id = ? AND session_id = ?
                        """,
                userId,
                sessionId
        );
        return affectedRows > 0;
    }

    private InterviewSessionResponse mapRow(ResultSet rs) throws SQLException {
        return new InterviewSessionResponse(
                rs.getString("session_id"),
                rs.getString("status"),
                rs.getString("summary"),
                rs.getString("first_question"),
                rs.getString("resume_text"),
                rs.getString("jd_text"),
                rs.getString("candidate_profile")
        );
    }

    private InterviewTurnRecord mapTurnRow(ResultSet rs) throws SQLException {
        return new InterviewTurnRecord(
                rs.getString("session_id"),
                rs.getInt("question_no"),
                rs.getString("question"),
                rs.getString("answer"),
                rs.getInt("duration_seconds"),
                rs.getString("ai_comment"),
                rs.getObject("score", Integer.class),
                rs.getString("scores"),
                rs.getBoolean("follow_up")
        );
    }

    private InterviewReportResponse mapReportRow(ResultSet rs) throws SQLException {
        return new InterviewReportResponse(
                rs.getString("session_id"),
                rs.getObject("total_score", Integer.class),
                rs.getString("report_content"),
                rs.getString("user_reflection"),
                rs.getString("summary"),
                formatTimestamp(rs.getTimestamp("updated_at")),
                rs.getString("scores"),
                rs.getString("strengths"),
                rs.getString("weaknesses"),
                rs.getString("advice")
        );
    }

    private String formatTimestamp(Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toLocalDateTime().toString().replace("T", " ");
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return null;
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }
}
