package com.faceai.pdfreader.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AgentConversationRepository {

    private final JdbcTemplate jdbcTemplate;

    public AgentConversationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String createSession(Long userId, String title) {
        String sessionId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        jdbcTemplate.update("""
                INSERT INTO agent_sessions (user_id, session_id, title, status)
                VALUES (?, ?, ?, 'ACTIVE')
                """, userId, sessionId, title);
        return sessionId;
    }

    public boolean sessionExists(Long userId, String sessionId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM agent_sessions WHERE user_id = ? AND session_id = ?",
                Integer.class, userId, sessionId
        );
        return count != null && count > 0;
    }

    public void updateSessionTitle(String sessionId, String title) {
        jdbcTemplate.update(
                "UPDATE agent_sessions SET title = ? WHERE session_id = ?",
                title, sessionId
        );
    }

    public void saveMessage(String sessionId, String role, String content,
                            String toolName, String toolCallId, String toolArguments) {
        jdbcTemplate.update("""
                INSERT INTO agent_messages (session_id, role, content, tool_name, tool_call_id, tool_arguments)
                VALUES (?, ?, ?, ?, ?, ?)
                """, sessionId, role, content, toolName, toolCallId, toolArguments);
    }

    public List<AgentMessageRecord> findMessages(String sessionId) {
        return jdbcTemplate.query("""
                SELECT role, content, tool_name, tool_call_id, tool_arguments, created_at
                FROM agent_messages
                WHERE session_id = ?
                ORDER BY id ASC
                """, (rs, rowNum) -> mapMessageRow(rs), sessionId);
    }

    public List<AgentSessionRecord> listSessions(Long userId) {
        return jdbcTemplate.query("""
                SELECT session_id, title, status, created_at, updated_at
                FROM agent_sessions
                WHERE user_id = ?
                ORDER BY updated_at DESC
                """, (rs, rowNum) -> mapSessionRow(rs), userId);
    }

    public boolean deleteSession(Long userId, String sessionId) {
        jdbcTemplate.update("""
                DELETE FROM agent_messages WHERE session_id = ?
                """, sessionId);
        int affected = jdbcTemplate.update("""
                DELETE FROM agent_sessions WHERE user_id = ? AND session_id = ?
                """, userId, sessionId);
        return affected > 0;
    }

    private AgentMessageRecord mapMessageRow(ResultSet rs) throws SQLException {
        return new AgentMessageRecord(
                rs.getString("role"),
                rs.getString("content"),
                rs.getString("tool_name"),
                rs.getString("tool_call_id"),
                rs.getString("tool_arguments"),
                formatTimestamp(rs.getTimestamp("created_at"))
        );
    }

    private AgentSessionRecord mapSessionRow(ResultSet rs) throws SQLException {
        return new AgentSessionRecord(
                rs.getString("session_id"),
                rs.getString("title"),
                rs.getString("status"),
                formatTimestamp(rs.getTimestamp("created_at")),
                formatTimestamp(rs.getTimestamp("updated_at"))
        );
    }

    private String formatTimestamp(Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toLocalDateTime().toString().replace("T", " ");
    }

    public record AgentMessageRecord(
            String role, String content, String toolName, String toolCallId, String toolArguments, String createdAt
    ) {}

    public record AgentSessionRecord(
            String sessionId, String title, String status, String createdAt, String updatedAt
    ) {}
}
