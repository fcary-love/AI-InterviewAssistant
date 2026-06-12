package com.faceai.pdfreader.model;

import java.util.List;

public record ResumeVersionCompareResponse(
        ResumeVersionDetailResponse left,
        ResumeVersionDetailResponse right,
        Integer textLengthDelta,
        List<String> addedKeywords,
        List<String> removedKeywords,
        String summary
) {
}
