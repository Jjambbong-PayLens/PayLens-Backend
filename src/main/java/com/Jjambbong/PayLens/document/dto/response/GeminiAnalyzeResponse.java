package com.Jjambbong.PayLens.document.dto.response;

import com.Jjambbong.PayLens.document.domain.Analysis;
import com.Jjambbong.PayLens.document.domain.AnalysisStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GeminiAnalyzeResponse {

    private final Long analysisId;
    private final AnalysisStatus status;
    private final Object result;

    public static GeminiAnalyzeResponse of(Analysis analysis, ObjectMapper objectMapper) {
        return GeminiAnalyzeResponse.builder()
                .analysisId(analysis.getId())
                .status(analysis.getStatus())
                .result(convertStringToJson(analysis.getResultJson(), objectMapper))
                .build();
    }

    private static Object convertStringToJson(String jsonString, ObjectMapper objectMapper) {
        if (jsonString == null || jsonString.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(jsonString, Object.class);
        } catch (Exception e) {
            return jsonString;
        }
    }
}
