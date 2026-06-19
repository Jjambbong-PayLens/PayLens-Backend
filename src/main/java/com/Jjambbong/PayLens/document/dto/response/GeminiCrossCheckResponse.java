package com.Jjambbong.PayLens.document.dto.response;

import com.Jjambbong.PayLens.document.domain.Analysis;
import com.Jjambbong.PayLens.document.domain.AnalysisStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GeminiCrossCheckResponse {

    private final Long analysisId;
    private final AnalysisStatus status;
    private final boolean autoAnalysisAvailable;
    private final boolean userReviewRequired;
    private final boolean recaptureRequired;

    public static GeminiCrossCheckResponse of(Analysis analysis, boolean autoAnalysisAvailable,
                                              boolean userReviewRequired, boolean recaptureRequired) {
        return GeminiCrossCheckResponse.builder()
                .analysisId(analysis.getId())
                .status(analysis.getStatus())
                .autoAnalysisAvailable(autoAnalysisAvailable)
                .userReviewRequired(userReviewRequired)
                .recaptureRequired(recaptureRequired)
                .build();
    }
}
