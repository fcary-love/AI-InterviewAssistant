package com.faceai.pdfreader.model;

import java.util.List;

public record QuestionBankOverviewResponse(
        Integer total,
        List<QuestionFacetResponse> directions,
        List<QuestionFacetResponse> categories,
        List<QuestionFacetResponse> difficulties
) {
}
