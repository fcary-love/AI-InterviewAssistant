package com.faceai.pdfreader.repository;

import com.faceai.pdfreader.model.DocumentRecord;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DocumentRepository {

    private final JdbcTemplate jdbcTemplate;

    public DocumentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(Long userId, String fileId, String fileName, String fileType, String fileUrl, String fullText) {
        jdbcTemplate.update("""
                        INSERT INTO document_files (user_id, file_id, file_name, file_type, file_url, full_text)
                        VALUES (?, ?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            user_id = VALUES(user_id),
                            file_name = VALUES(file_name),
                            file_type = VALUES(file_type),
                            file_url = VALUES(file_url),
                            full_text = VALUES(full_text)
                        """,
                userId,
                fileId,
                fileName,
                fileType,
                fileUrl,
                fullText
        );
    }

    public Optional<DocumentRecord> findByFileId(Long userId, String fileId) {
        return jdbcTemplate.query("""
                        SELECT id, file_id, file_name, file_type, file_url, full_text, created_at
                        FROM document_files
                        WHERE user_id = ? AND file_id = ?
                        """,
                (rs, rowNum) -> mapRow(rs),
                userId,
                fileId
        ).stream().findFirst();
    }

    public void updateFullText(Long userId, String fileId, String fullText) {
        jdbcTemplate.update("""
                        UPDATE document_files
                        SET full_text = ?
                        WHERE user_id = ? AND file_id = ?
                        """,
                fullText,
                userId,
                fileId
        );
    }

    private DocumentRecord mapRow(ResultSet rs) throws SQLException {
        return new DocumentRecord(
                rs.getLong("id"),
                rs.getString("file_id"),
                rs.getString("file_name"),
                rs.getString("file_type"),
                rs.getString("file_url"),
                rs.getString("full_text"),
                rs.getObject("created_at", LocalDateTime.class)
        );
    }
}
