package com.Jjambbong.PayLens.document.controller;

import com.Jjambbong.PayLens.document.dto.response.AnalysisResponse;
import com.Jjambbong.PayLens.document.service.DocumentService;
import com.Jjambbong.PayLens.global.api.ApiResponse;
import com.Jjambbong.PayLens.global.api.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.Jjambbong.PayLens.document.dto.response.AnalysisListItemResponse;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analyses")
@Tag(name = "Analysis", description = "문서 분석 결과 조회 API")
public class AnalysisController {

    private final DocumentService documentService;

    @GetMapping
    @Operation(summary = "내 분석 리포트 목록 조회", description = "내가 분석 요청했던 모든 리포트 목록을 최신순으로 조회합니다.")
    public ApiResponse<List<AnalysisListItemResponse>> getMyAnalyses(
            @AuthenticationPrincipal Long userId) {
        List<AnalysisListItemResponse> response = documentService.getMyAnalyses(userId);
        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }

    @GetMapping("/document/{documentId}")
    @Operation(summary = "문서 분석 결과 상세 조회", description = "특정 문서 ID에 연결된 AI 분석 결과를 조회합니다.")
    public ApiResponse<AnalysisResponse> getAnalysisResult(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long documentId) {
        AnalysisResponse response = documentService.getAnalysisResult(userId, documentId);
        return ApiResponse.onSuccess(SuccessCode.DOCUMENT_ANALYZE_GET_SUCCESS, response);
    }

    @GetMapping("/{analysisId}")
    @Operation(summary = "분석 ID 기반 결과 상세 조회", description = "특정 분석 ID에 해당하는 AI 분석 결과를 조회합니다.")
    public ApiResponse<AnalysisResponse> getAnalysisResultByAnalysisId(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long analysisId) {
        AnalysisResponse response = documentService.getAnalysisResultByAnalysisId(userId, analysisId);
        return ApiResponse.onSuccess(SuccessCode.DOCUMENT_ANALYZE_GET_SUCCESS, response);
    }

    @DeleteMapping("/{analysisId}")
    @Operation(summary = "분석 리포트 삭제", description = "특정 분석 리포트를 삭제합니다. 연결된 문서들은 삭제되지 않습니다.")
    public ApiResponse<Void> deleteAnalysis(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long analysisId) {
        documentService.deleteAnalysis(userId, analysisId);
        return ApiResponse.onSuccess(SuccessCode.OK, null); // 또는 전용 SuccessCode 생성
    }
}
