package com.Jjambbong.PayLens.document.dto.response;

import com.Jjambbong.PayLens.document.domain.Analysis;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AnalysisListItemResponse {
    private final Long analysisId;
    private final String representativeDocumentName; // 대표 문서 이름
    private final int documentCount; // 분석에 사용된 문서 개수
    private final LocalDateTime createdAt;

    public AnalysisListItemResponse(Analysis analysis) {
        this.analysisId = analysis.getId();
        this.createdAt = analysis.getCreatedAt();
        this.documentCount = analysis.getDocuments().size();
        this.representativeDocumentName = (analysis.getDocuments() != null && !analysis.getDocuments().isEmpty())
                ? analysis.getDocuments().get(0).getOriginalFileName()
                : "연결된 문서 없음";
    }
}