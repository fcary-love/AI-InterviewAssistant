package com.faceai.pdfreader.controller;

import com.faceai.pdfreader.auth.AuthContext;
import com.faceai.pdfreader.model.ApiResponse;
import com.faceai.pdfreader.service.KnowledgeGraphService;
import com.faceai.pdfreader.service.KnowledgeGraphService.SkillTreeNode;
import com.faceai.pdfreader.service.KnowledgeGraphService.StudySuggestion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "知识图谱", description = "技能树、掌握度、学习建议")
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeGraphService knowledgeGraphService;

    public KnowledgeController(KnowledgeGraphService knowledgeGraphService) {
        this.knowledgeGraphService = knowledgeGraphService;
    }

    /**
     * 获取技能树（含用户掌握度）
     */
    @GetMapping("/tree")
    public ApiResponse<List<SkillTreeNode>> getSkillTree(
            @RequestParam(defaultValue = "后端开发") String direction) {
        Long userId = AuthContext.currentUserId();
        return ApiResponse.success(knowledgeGraphService.getUserSkillTree(userId, direction));
    }

    /**
     * 获取用户全部掌握度
     */
    @GetMapping("/mastery")
    public ApiResponse<List<Map<String, Object>>> getMastery() {
        Long userId = AuthContext.currentUserId();
        return ApiResponse.success(knowledgeGraphService.getWeakKnowledgePoints(userId));
    }

    /**
     * 获取薄弱知识点
     */
    @GetMapping("/weak-points")
    public ApiResponse<List<Map<String, Object>>> getWeakPoints() {
        Long userId = AuthContext.currentUserId();
        return ApiResponse.success(knowledgeGraphService.getWeakKnowledgePoints(userId));
    }

    /**
     * 获取学习建议
     */
    @GetMapping("/suggestions")
    public ApiResponse<List<StudySuggestion>> getSuggestions(
            @RequestParam(defaultValue = "后端开发") String direction) {
        Long userId = AuthContext.currentUserId();
        return ApiResponse.success(knowledgeGraphService.suggestNextStudy(userId, direction));
    }
}
