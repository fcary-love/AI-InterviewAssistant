package com.faceai.pdfreader.repository;

import com.faceai.pdfreader.model.record.InterviewerProfile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class InterviewerRepository {

    private final JdbcTemplate jdbc;

    public InterviewerRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<InterviewerProfile> findAll() {
        return jdbc.query(
            "SELECT * FROM interviewer_profiles ORDER BY is_default DESC, id ASC",
            this::mapRow
        );
    }

    public Optional<InterviewerProfile> findById(Long id) {
        var list = jdbc.query(
            "SELECT * FROM interviewer_profiles WHERE id = ?",
            this::mapRow,
            id
        );
        return list.stream().findFirst();
    }

    public Optional<InterviewerProfile> findDefault() {
        var list = jdbc.query(
            "SELECT * FROM interviewer_profiles WHERE is_default = 1 LIMIT 1",
            this::mapRow
        );
        return list.stream().findFirst();
    }

    private InterviewerProfile mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new InterviewerProfile(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("avatar"),
            rs.getString("personality"),
            rs.getString("style_desc"),
            rs.getString("greeting"),
            rs.getString("catchphrase"),
            rs.getBoolean("is_default"),
            rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null
        );
    }
}
