package com.faceai.pdfreader.model.record;

import java.time.LocalDateTime;

public record InterviewerProfile(
    Long id,
    String name,
    String avatar,
    String personality,
    String styleDesc,
    String greeting,
    String catchphrase,
    Boolean isDefault,
    LocalDateTime createdAt
) {}
