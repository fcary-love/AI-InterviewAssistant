package com.faceai.pdfreader.controller;

import com.faceai.pdfreader.model.ApiResponse;
import com.faceai.pdfreader.service.InterviewReplayService;
import com.faceai.pdfreader.service.InterviewReplayService.InterviewComparison;
import com.faceai.pdfreader.service.InterviewReplayService.KeywordAnnotation;
import com.faceai.pdfreader.service.InterviewReplayService.TimeAnalysis;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "面试回放", description = "关键词标注、时间分析、面试对比")
@Validated
@RestController
@RequestMapping("/api/interviews")
public class ReplayController {

    private final InterviewReplayService replayService;

    public ReplayController(InterviewReplayService replayService) {
        this.replayService = replayService;
    }

    /**
     * AI标注关键词命中
     */
    @PostMapping("/{sessionId}/keywords/annotate")
    public ApiResponse<List<KeywordAnnotation>> annotateKeywords(
            @PathVariable @NotBlank String sessionId) {
        return ApiResponse.success(replayService.annotateKeywords(sessionId));
    }

    /**
     * 获取关键词标注
     */
    @GetMapping("/{sessionId}/keywords")
    public ApiResponse<List<KeywordAnnotation>> getKeywords(
            @PathVariable @NotBlank String sessionId) {
        return ApiResponse.success(replayService.getAnnotations(sessionId));
    }

    /**
     * 时间分布分析
     */
    @GetMapping("/{sessionId}/time-analysis")
    public ApiResponse<TimeAnalysis> getTimeAnalysis(
            @PathVariable @NotBlank String sessionId) {
        return ApiResponse.success(replayService.analyzeTimeDistribution(sessionId));
    }

    /**
     * 对比两次面试
     */
    @PostMapping("/compare")
    public ApiResponse<InterviewComparison> compareInterviews(
            @RequestParam @NotBlank String sessionId1,
            @RequestParam @NotBlank String sessionId2) {
        return ApiResponse.success(replayService.compareInterviews(sessionId1, sessionId2));
    }
}
