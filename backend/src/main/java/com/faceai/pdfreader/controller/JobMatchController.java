package com.faceai.pdfreader.controller;

import com.faceai.pdfreader.model.ApiResponse;
import com.faceai.pdfreader.model.JobMatchDetailResponse;
import com.faceai.pdfreader.model.JobMatchHistoryResponse;
import com.faceai.pdfreader.model.JobMatchRequest;
import com.faceai.pdfreader.model.JobMatchResponse;
import com.faceai.pdfreader.service.JobMatchService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
public class JobMatchController {

    private final JobMatchService jobMatchService;

    public JobMatchController(JobMatchService jobMatchService) {
        this.jobMatchService = jobMatchService;
    }

    @PostMapping("/match")
    public ApiResponse<JobMatchResponse> analyze(@Valid @RequestBody JobMatchRequest request) {
        return ApiResponse.success(jobMatchService.analyze(request.resumeFileId(), request.jdText()));
    }

    @GetMapping
    public ApiResponse<List<JobMatchHistoryResponse>> list(@RequestParam(defaultValue = "30") Integer limit) {
        return ApiResponse.success(jobMatchService.listHistory(limit));
    }

    @GetMapping("/{id}")
    public ApiResponse<JobMatchDetailResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(jobMatchService.detail(id));
    }
}
