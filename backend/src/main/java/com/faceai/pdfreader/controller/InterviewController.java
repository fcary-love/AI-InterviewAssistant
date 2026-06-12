package com.faceai.pdfreader.controller;

import com.faceai.pdfreader.model.ApiResponse;
import com.faceai.pdfreader.model.InterviewAnswerRequest;
import com.faceai.pdfreader.model.InterviewReportRequest;
import com.faceai.pdfreader.model.InterviewReportResponse;
import com.faceai.pdfreader.model.InterviewReportSummaryResponse;
import com.faceai.pdfreader.model.InterviewSessionResponse;
import com.faceai.pdfreader.model.InterviewStartRequest;
import com.faceai.pdfreader.model.InterviewTurnRecord;
import com.faceai.pdfreader.model.InterviewTurnResponse;
import com.faceai.pdfreader.service.EloRatingService;
import com.faceai.pdfreader.service.InterviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

@Tag(name = "模拟面试", description = "面试会话、答题、评分、报告相关接口")
@Validated
@RestController
@RequestMapping("/api/interviews")
public class InterviewController {

    private final InterviewService interviewService;
    private final EloRatingService eloRatingService;

    public InterviewController(InterviewService interviewService, EloRatingService eloRatingService) {
        this.interviewService = interviewService;
        this.eloRatingService = eloRatingService;
    }

    @Operation(summary = "开始面试", description = "创建新的面试会话，返回第一题")
    @PostMapping("/start")
    public ApiResponse<InterviewSessionResponse> start(@Valid @RequestBody InterviewStartRequest request) {
        return ApiResponse.success(interviewService.start(request));
    }

    @Operation(summary = "获取面试会话", description = "根据会话ID获取面试详情")
    @GetMapping("/{sessionId}")
    public ApiResponse<InterviewSessionResponse> get(@PathVariable @NotBlank String sessionId) {
        return ApiResponse.success(interviewService.get(sessionId));
    }

    @Operation(summary = "提交答案", description = "同步提交答案并获取评分和下一题")
    @PostMapping("/{sessionId}/answer")
    public ApiResponse<InterviewTurnResponse> answer(
            @PathVariable @NotBlank String sessionId,
            @RequestBody InterviewAnswerRequest request
    ) {
        return ApiResponse.success(interviewService.submitAnswer(sessionId, request));
    }

    @Operation(summary = "流式提交答案", description = "SSE流式提交答案，逐字输出下一题")
    @PostMapping(value = "/{sessionId}/answer/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamAnswer(
            @PathVariable @NotBlank String sessionId,
            @RequestBody InterviewAnswerRequest request
    ) {
        return interviewService.streamAnswer(sessionId, request)
                .map(token -> ServerSentEvent.<String>builder().data(token).build())
                .concatWithValues(ServerSentEvent.<String>builder().event("done").data("[DONE]").build());
    }

    @Operation(summary = "生成报告", description = "生成面试报告")
    @PostMapping("/{sessionId}/report")
    public ApiResponse<InterviewReportResponse> generateReport(
            @PathVariable @NotBlank String sessionId,
            @RequestBody(required = false) InterviewReportRequest request
    ) {
        String reflection = request == null ? "" : request.userReflection();
        return ApiResponse.success(interviewService.generateReport(sessionId, reflection));
    }

    @Operation(summary = "AI优化报告", description = "使用AI深度分析并优化面试报告")
    @PostMapping("/{sessionId}/report/refine")
    public ApiResponse<InterviewReportResponse> refineReport(
            @PathVariable @NotBlank String sessionId,
            @RequestBody(required = false) InterviewReportRequest request
    ) {
        String reflection = request == null ? "" : request.userReflection();
        return ApiResponse.success(interviewService.refineReport(sessionId, reflection));
    }

    @Operation(summary = "获取报告", description = "获取面试报告详情")
    @GetMapping("/{sessionId}/report")
    public ApiResponse<InterviewReportResponse> getReport(@PathVariable @NotBlank String sessionId) {
        return ApiResponse.success(interviewService.getReport(sessionId));
    }

    @Operation(summary = "获取答题记录", description = "获取面试的所有答题记录")
    @GetMapping("/{sessionId}/turns")
    public ApiResponse<List<InterviewTurnRecord>> listTurns(@PathVariable @NotBlank String sessionId) {
        return ApiResponse.success(interviewService.listTurns(sessionId));
    }

    @Operation(summary = "报告列表", description = "获取用户的所有面试报告")
    @GetMapping("/reports")
    public ApiResponse<List<InterviewReportSummaryResponse>> listReports() {
        return ApiResponse.success(interviewService.listReports());
    }

    @Operation(summary = "删除报告", description = "删除指定面试报告")
    @DeleteMapping("/{sessionId}/report")
    public ApiResponse<Void> deleteReport(@PathVariable @NotBlank String sessionId) {
        interviewService.deleteReport(sessionId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "Elo难度轨迹", description = "获取面试的自适应难度变化轨迹")
    @GetMapping("/{sessionId}/elo-trajectory")
    public ApiResponse<List<EloRatingService.DifficultyTrajectory>> getEloTrajectory(
            @PathVariable @NotBlank String sessionId) {
        Long userId = com.faceai.pdfreader.auth.AuthContext.currentUserId();
        return ApiResponse.success(eloRatingService.getTrajectories(userId, sessionId));
    }
}
