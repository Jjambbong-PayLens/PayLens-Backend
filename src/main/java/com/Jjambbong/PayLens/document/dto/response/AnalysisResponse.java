package com.Jjambbong.PayLens.document.dto.response;

import com.Jjambbong.PayLens.document.domain.Analysis;
import com.Jjambbong.PayLens.document.domain.Document;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class AnalysisResponse {
    private final Long analysisId;
    private final List<Long> documentIds; // 여러 문서 ID를 담도록 수정
    private final Object result;

    public AnalysisResponse(Analysis analysis, ObjectMapper objectMapper) {
        this.analysisId = analysis.getId();
        this.documentIds = analysis.getDocuments().stream()
                .map(Document::getId)
                .collect(Collectors.toList());
        this.result = convertStringToJson(analysis.getResultJson(), objectMapper);
    }

    private Object convertStringToJson(String jsonString, ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(jsonString, Object.class);
        } catch (JsonProcessingException e) {
            return jsonString;
        }
    }
}
