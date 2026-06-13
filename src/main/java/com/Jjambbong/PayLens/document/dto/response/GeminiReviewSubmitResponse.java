package com.Jjambbong.PayLens.document.dto.response;

import com.Jjambbong.PayLens.document.domain.Analysis;
import com.Jjambbong.PayLens.document.domain.AnalysisStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GeminiReviewSubmitResponse {

    private final Long analysisId;
    private final AnalysisStatus status;

    public static GeminiReviewSubmitResponse from(Analysis analysis) {
        return GeminiReviewSubmitResponse.builder()
                .analysisId(analysis.getId())
                .status(analysis.getStatus())
                .build();
    }
}
