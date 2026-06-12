package com.faceai.pdfreader.controller;

import com.faceai.pdfreader.model.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/")
    public ApiResponse<String> root() {
        return ApiResponse.success("AI 面试助手服务运行中");
    }

    @GetMapping("/api/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("ok");
    }
}
