package com.Jjambbong.PayLens.document.dto.response;

import com.Jjambbong.PayLens.document.domain.AnalysisStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GeminiReviewResponse {

    private final Long analysisId;
    private final AnalysisStatus status;
    private final Object documentGroups;
}
