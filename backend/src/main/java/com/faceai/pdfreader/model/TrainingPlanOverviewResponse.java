package com.faceai.pdfreader.model;

import java.util.List;

public record TrainingPlanOverviewResponse(
        Integer total,
        Integer todo,
        Integer doing,
        Integer done,
        Integer completionRate,
        String latestAction,
        List<String> weakCategories
) {
}
