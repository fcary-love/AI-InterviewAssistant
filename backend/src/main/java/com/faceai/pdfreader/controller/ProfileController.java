package com.faceai.pdfreader.controller;

import com.faceai.pdfreader.model.ApiResponse;
import com.faceai.pdfreader.model.ProfileOverviewResponse;
import com.faceai.pdfreader.model.ResumeVersionCompareResponse;
import com.faceai.pdfreader.model.ResumeVersionDetailResponse;
import com.faceai.pdfreader.model.ResumeVersionResponse;
import com.faceai.pdfreader.service.ProfileService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/overview")
    public ApiResponse<ProfileOverviewResponse> overview() {
        return ApiResponse.success(profileService.overview());
    }

    @GetMapping("/resumes")
    public ApiResponse<List<ResumeVersionResponse>> resumes() {
        return ApiResponse.success(profileService.listResumeVersions());
    }

    @GetMapping("/resumes/{id}")
    public ApiResponse<ResumeVersionDetailResponse> resumeDetail(@PathVariable Long id) {
        return ApiResponse.success(profileService.getResumeVersion(id));
    }

    @GetMapping("/resumes/compare")
    public ApiResponse<ResumeVersionCompareResponse> compare(
            @RequestParam Long leftId,
            @RequestParam Long rightId
    ) {
        return ApiResponse.success(profileService.compareResumeVersions(leftId, rightId));
    }
}
