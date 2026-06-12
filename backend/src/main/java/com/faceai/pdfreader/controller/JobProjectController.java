package com.faceai.pdfreader.controller;

import com.faceai.pdfreader.model.ApiResponse;
import com.faceai.pdfreader.model.JobProjectCreateRequest;
import com.faceai.pdfreader.model.JobProjectDetailResponse;
import com.faceai.pdfreader.model.JobProjectStatusRequest;
import com.faceai.pdfreader.model.JobProjectSummaryResponse;
import com.faceai.pdfreader.service.JobProjectService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/job-projects")
public class JobProjectController {

    private final JobProjectService jobProjectService;

    public JobProjectController(JobProjectService jobProjectService) {
        this.jobProjectService = jobProjectService;
    }

    @GetMapping
    public ApiResponse<List<JobProjectSummaryResponse>> list(@RequestParam(defaultValue = "30") Integer limit) {
        return ApiResponse.success(jobProjectService.list(limit));
    }

    @PostMapping
    public ApiResponse<JobProjectDetailResponse> create(@Valid @RequestBody JobProjectCreateRequest request) {
        return ApiResponse.success(jobProjectService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<JobProjectDetailResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(jobProjectService.detail(id));
    }

    @PostMapping("/{id}/match")
    public ApiResponse<JobProjectDetailResponse> match(@PathVariable Long id) {
        return ApiResponse.success(jobProjectService.match(id));
    }

    @PostMapping("/{id}/tailored-resume")
    public ApiResponse<JobProjectDetailResponse> generateTailoredResume(@PathVariable Long id) {
        return ApiResponse.success(jobProjectService.generateTailoredResume(id));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<JobProjectDetailResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody JobProjectStatusRequest request
    ) {
        return ApiResponse.success(jobProjectService.updateStatus(id, request.status()));
    }
}
