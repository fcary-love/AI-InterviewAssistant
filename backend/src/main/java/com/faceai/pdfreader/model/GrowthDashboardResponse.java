package com.faceai.pdfreader.model;

import java.util.List;

public record GrowthDashboardResponse(
        List<GrowthMetricResponse> metrics,
        List<ScoreTrendPointResponse> scoreTrend,
        List<WeakPointResponse> weakPoints,
        String latestAction,
        String summary
) {
}
