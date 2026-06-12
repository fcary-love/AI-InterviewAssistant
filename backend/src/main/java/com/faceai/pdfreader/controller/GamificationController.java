package com.faceai.pdfreader.controller;

import com.faceai.pdfreader.auth.AuthContext;
import com.faceai.pdfreader.service.GamificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "游戏化系统", description = "等级、成就、每日任务")
@RestController
@RequestMapping("/api/gamification")
public class GamificationController {

    private final GamificationService service;

    public GamificationController(GamificationService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        Long userId = AuthContext.currentUserId();
        var summary = service.getSummary(userId);
        return ResponseEntity.ok(Map.of("code", 200, "data", summary));
    }

    @GetMapping("/achievements")
    public ResponseEntity<Map<String, Object>> getAchievements() {
        Long userId = AuthContext.currentUserId();
        var achievements = service.getAchievements(userId);
        return ResponseEntity.ok(Map.of("code", 200, "data", achievements));
    }

    @GetMapping("/daily-tasks")
    public ResponseEntity<Map<String, Object>> getDailyTasks() {
        Long userId = AuthContext.currentUserId();
        var summary = service.getSummary(userId);
        return ResponseEntity.ok(Map.of("code", 200, "data", summary.dailyTasks()));
    }

    @PostMapping("/daily-tasks/claim")
    public ResponseEntity<Map<String, Object>> claimDailyTask(@RequestBody Map<String, String> body) {
        Long userId = AuthContext.currentUserId();
        String taskType = body.get("taskType");
        if (taskType == null || taskType.isBlank()) {
            return ResponseEntity.ok(Map.of("code", 400, "message", "任务类型不能为空"));
        }
        int expGained = service.claimDailyTask(userId, taskType);
        if (expGained == 0) {
            return ResponseEntity.ok(Map.of("code", 400, "message", "任务未完成或已领取"));
        }
        return ResponseEntity.ok(Map.of("code", 200, "data", Map.of("expGained", expGained)));
    }
}
