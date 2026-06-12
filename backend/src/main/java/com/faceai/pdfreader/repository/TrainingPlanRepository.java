package com.faceai.pdfreader.repository;

import com.faceai.pdfreader.model.TrainingPlanOverviewResponse;
import com.faceai.pdfreader.model.TrainingTaskResponse;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;

@Repository
public class TrainingPlanRepository {

    private final JdbcTemplate jdbcTemplate;

    public TrainingPlanRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public TrainingPlanOverviewResponse overview(Long userId) {
        int total = countByStatus(userId, null);
        int todo = countByStatus(userId, "TODO");
        int doing = countByStatus(userId, "DOING");
        int done = countByStatus(userId, "DONE");
        int completionRate = total == 0 ? 0 : Math.round(done * 100f / total);
        String latestAction = jdbcTemplate.query("""
                        SELECT title
                        FROM training_tasks
                        WHERE user_id = ?
                        ORDER BY updated_at DESC
                        LIMIT 1
                        """,
                (rs, rowNum) -> rs.getString("title"),
                userId
        ).stream().findFirst().orElse("先生成一份训练计划");
        return new TrainingPlanOverviewResponse(
                total,
                todo,
                doing,
                done,
                completionRate,
                latestAction,
                weakCategories(userId, 5)
        );
    }

    public List<TrainingTaskResponse> listTasks(Long userId) {
        return jdbcTemplate.query("""
                        SELECT id, title, description, task_type, category, target_count, finished_count,
                               status, source, due_date, created_at, updated_at
                        FROM training_tasks
                        WHERE user_id = ?
                        ORDER BY
                          FIELD(status, 'DOING', 'TODO', 'DONE'),
                          COALESCE(due_date, DATE_ADD(CURRENT_DATE, INTERVAL 30 DAY)) ASC,
                          updated_at DESC
                        """,
                (rs, rowNum) -> mapRow(rs),
                userId
        );
    }

    public List<String> weakCategories(Long userId, int limit) {
        return jdbcTemplate.query("""
                        SELECT COALESCE(q.category, '综合复盘') AS category, COUNT(*) AS total
                        FROM interview_turns t
                        INNER JOIN interview_sessions s ON s.session_id = t.session_id
                        LEFT JOIN interview_questions q ON q.question_text = t.question
                        WHERE s.user_id = ? AND (t.score IS NULL OR t.score < 70)
                        GROUP BY COALESCE(q.category, '综合复盘')
                        ORDER BY total DESC, category ASC
                        LIMIT ?
                        """,
                (rs, rowNum) -> rs.getString("category"),
                userId,
                Math.max(1, Math.min(limit, 10))
        );
    }

    public boolean hasOpenTask(Long userId, String title) {
        Integer total = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM training_tasks
                        WHERE user_id = ? AND title = ? AND status <> 'DONE'
                        """,
                Integer.class,
                userId,
                title
        );
        return total != null && total > 0;
    }

    public TrainingTaskResponse createTask(
            Long userId,
            String title,
            String description,
            String taskType,
            String category,
            Integer targetCount,
            String source,
            LocalDate dueDate
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                            INSERT INTO training_tasks
                              (user_id, title, description, task_type, category, target_count, finished_count, status, source, due_date)
                            VALUES (?, ?, ?, ?, ?, ?, 0, 'TODO', ?, ?)
                            """,
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, userId);
            statement.setString(2, title);
            statement.setString(3, description);
            statement.setString(4, taskType);
            statement.setString(5, category);
            statement.setInt(6, targetCount == null ? 1 : Math.max(1, targetCount));
            statement.setString(7, source);
            statement.setDate(8, dueDate == null ? null : Date.valueOf(dueDate));
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("训练任务创建失败");
        }
        return findById(userId, key.longValue());
    }

    public TrainingTaskResponse updateStatus(Long userId, Long taskId, String status) {
        String normalizedStatus = normalizeStatus(status);
        int updated = jdbcTemplate.update("""
                        UPDATE training_tasks
                        SET status = ?,
                            finished_count = CASE
                                WHEN ? = 'DONE' THEN target_count
                                WHEN ? = 'TODO' THEN 0
                                ELSE finished_count
                            END
                        WHERE user_id = ? AND id = ?
                        """,
                normalizedStatus,
                normalizedStatus,
                normalizedStatus,
                userId,
                taskId
        );
        if (updated == 0) {
            throw new IllegalArgumentException("训练任务不存在");
        }
        return findById(userId, taskId);
    }

    public void deleteTask(Long userId, Long taskId) {
        jdbcTemplate.update("DELETE FROM training_tasks WHERE user_id = ? AND id = ?", userId, taskId);
    }

    private TrainingTaskResponse findById(Long userId, Long taskId) {
        return jdbcTemplate.query("""
                        SELECT id, title, description, task_type, category, target_count, finished_count,
                               status, source, due_date, created_at, updated_at
                        FROM training_tasks
                        WHERE user_id = ? AND id = ?
                        """,
                (rs, rowNum) -> mapRow(rs),
                userId,
                taskId
        ).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("训练任务不存在"));
    }

    private int countByStatus(Long userId, String status) {
        Number value;
        if (status == null) {
            value = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM training_tasks WHERE user_id = ?", Number.class, userId);
        } else {
            value = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM training_tasks WHERE user_id = ? AND status = ?",
                    Number.class,
                    userId,
                    status
            );
        }
        return value == null ? 0 : value.intValue();
    }

    private String normalizeStatus(String status) {
        if ("TODO".equals(status) || "DOING".equals(status) || "DONE".equals(status)) {
            return status;
        }
        throw new IllegalArgumentException("训练任务状态只能是 TODO、DOING 或 DONE");
    }

    private TrainingTaskResponse mapRow(ResultSet rs) throws SQLException {
        Date dueDate = rs.getDate("due_date");
        return new TrainingTaskResponse(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("task_type"),
                rs.getString("category"),
                rs.getObject("target_count", Integer.class),
                rs.getObject("finished_count", Integer.class),
                rs.getString("status"),
                rs.getString("source"),
                dueDate == null ? "" : dueDate.toLocalDate().toString(),
                formatTimestamp(rs.getTimestamp("created_at")),
                formatTimestamp(rs.getTimestamp("updated_at"))
        );
    }

    private String formatTimestamp(Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toLocalDateTime().toString().replace("T", " ");
    }
}
