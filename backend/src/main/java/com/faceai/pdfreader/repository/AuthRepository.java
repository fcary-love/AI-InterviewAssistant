package com.faceai.pdfreader.repository;

import com.faceai.pdfreader.auth.AuthUser;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class AuthRepository {

    private final JdbcTemplate jdbcTemplate;

    public AuthRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UserPasswordRecord> findPasswordByUsername(String username) {
        return jdbcTemplate.query("""
                        SELECT u.id, u.username, u.password_hash, p.display_name
                        FROM app_users u
                        LEFT JOIN user_profiles p ON p.id = u.id
                        WHERE u.username = ? AND u.status = 'ACTIVE'
                        """,
                (rs, rowNum) -> new UserPasswordRecord(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("display_name"),
                        rs.getString("password_hash")
                ),
                username
        ).stream().findFirst();
    }

    public Optional<AuthUser> findById(Long id) {
        return jdbcTemplate.query("""
                        SELECT u.id, u.username, p.display_name
                        FROM app_users u
                        LEFT JOIN user_profiles p ON p.id = u.id
                        WHERE u.id = ? AND u.status = 'ACTIVE'
                        """,
                (rs, rowNum) -> new AuthUser(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("display_name")
                ),
                id
        ).stream().findFirst();
    }

    public boolean existsByUsername(String username) {
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM app_users WHERE username = ?",
                Integer.class,
                username
        );
        return total != null && total > 0;
    }

    @Transactional
    public AuthUser createUser(String username, String passwordHash, String displayName) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                            INSERT INTO user_profiles (display_name, target_role, skill_keywords)
                            VALUES (?, 'Java 后端开发', 'Java, Spring Boot, MySQL, Redis, Vue3, Docker')
                            """,
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, displayName);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("用户资料创建失败");
        }
        Long userId = key.longValue();
        jdbcTemplate.update("""
                        INSERT INTO app_users (id, username, password_hash)
                        VALUES (?, ?, ?)
                        """,
                userId,
                username,
                passwordHash
        );
        return new AuthUser(userId, username, displayName);
    }

    public record UserPasswordRecord(
            Long id,
            String username,
            String displayName,
            String passwordHash
    ) {
    }
}
