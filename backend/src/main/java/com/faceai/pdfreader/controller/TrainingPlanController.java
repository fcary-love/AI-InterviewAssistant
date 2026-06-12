package com.faceai.pdfreader.controller;

import com.faceai.pdfreader.model.ApiResponse;
import com.faceai.pdfreader.model.TrainingGenerateResponse;
import com.faceai.pdfreader.model.TrainingPlanOverviewResponse;
import com.faceai.pdfreader.model.TrainingTaskResponse;
import com.faceai.pdfreader.model.TrainingTaskStatusRequest;
import com.faceai.pdfreader.service.TrainingPlanService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/training")
public class TrainingPlanController {

    private final TrainingPlanService trainingPlanService;

    public TrainingPlanController(TrainingPlanService trainingPlanService) {
        this.trainingPlanService = trainingPlanService;
    }

    @GetMapping("/overview")
    public ApiResponse<TrainingPlanOverviewResponse> overview() {
        return ApiResponse.success(trainingPlanService.overview());
    }

    @GetMapping("/tasks")
    public ApiResponse<List<TrainingTaskResponse>> tasks() {
        return ApiResponse.success(trainingPlanService.listTasks());
    }

    @PostMapping("/generate")
    public ApiResponse<TrainingGenerateResponse> generate() {
        return ApiResponse.success(trainingPlanService.generatePlan());
    }

    @PatchMapping("/tasks/{id}/status")
    public ApiResponse<TrainingTaskResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody TrainingTaskStatusRequest request
    ) {
        return ApiResponse.success(trainingPlanService.updateStatus(id, request.status()));
    }

    @DeleteMapping("/tasks/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        trainingPlanService.deleteTask(id);
        return ApiResponse.success(null);
    }
}
