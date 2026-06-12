package com.faceai.pdfreader.controller;

import com.faceai.pdfreader.model.ApiResponse;
import com.faceai.pdfreader.model.GrowthDashboardResponse;
import com.faceai.pdfreader.service.GrowthDashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class GrowthDashboardController {

    private final GrowthDashboardService growthDashboardService;

    public GrowthDashboardController(GrowthDashboardService growthDashboardService) {
        this.growthDashboardService = growthDashboardService;
    }

    @GetMapping("/growth")
    public ApiResponse<GrowthDashboardResponse> growth() {
        return ApiResponse.success(growthDashboardService.overview());
    }
}
