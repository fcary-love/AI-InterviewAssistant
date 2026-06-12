package com.faceai.pdfreader.model;

import java.util.List;

public record ProfileOverviewResponse(
        UserProfileResponse profile,
        List<ResumeVersionResponse> resumeVersions,
        List<JobMatchHistoryResponse> jobMatches,
        Integer resumeCount,
        Integer jobMatchCount,
        Integer averageMatchScore
) {
}
