package com.Jjambbong.PayLens.document.controller;

import com.Jjambbong.PayLens.document.dto.request.DocumentAnalyzeRequest;
import com.Jjambbong.PayLens.document.dto.request.GeminiReviewSubmitRequest;
import com.Jjambbong.PayLens.document.dto.response.GeminiAnalyzeResponse;
import com.Jjambbong.PayLens.document.dto.response.GeminiCrossCheckResponse;
import com.Jjambbong.PayLens.document.dto.response.GeminiReviewResponse;
import com.Jjambbong.PayLens.document.dto.response.GeminiReviewSubmitResponse;
import com.Jjambbong.PayLens.document.service.GeminiPipelineService;
import com.Jjambbong.PayLens.document.service.GeminiService;
import com.Jjambbong.PayLens.global.api.ApiResponse;
import com.Jjambbong.PayLens.global.api.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/gemini")
public class GeminiController {

    private final GeminiService geminiService;
    private final GeminiPipelineService geminiPipelineService;

    @PostMapping("/cross-check")
    @Operation(summary = "문서 OCR-Gemini 교차 검증", description = "OCR 완료 문서를 대상으로 Gemini가 문서 유형 분류, 필드 추출, 사용자 검증 필요 여부를 판단합니다.")
    public ApiResponse<GeminiCrossCheckResponse> crossCheckDocuments(
            @AuthenticationPrincipal Long userId,
            @RequestBody DocumentAnalyzeRequest request) {
        GeminiCrossCheckResponse response = geminiPipelineService.crossCheck(userId, request);
        return ApiResponse.onSuccess(SuccessCode.GEMINI_CROSS_CHECK_SUCCESS, response);
    }

    @GetMapping("/{analysisId}/review")
    @Operation(summary = "Gemini 사용자 검증 정보 조회", description = "사용자 검증 화면에 필요한 추출 필드와 원본 문서 presigned GET URL을 조회합니다.")
    public ApiResponse<GeminiReviewResponse> getReview(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long analysisId) {
        GeminiReviewResponse response = geminiPipelineService.getReview(userId, analysisId);
        return ApiResponse.onSuccess(SuccessCode.GEMINI_REVIEW_GET_SUCCESS, response);
    }

    @PostMapping("/{analysisId}/review")
    @Operation(summary = "Gemini 사용자 검증 제출", description = "사용자가 확인/수정한 필드를 저장하고 최종 분석 준비 상태로 변경합니다.")
    public ApiResponse<GeminiReviewSubmitResponse> submitReview(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long analysisId,
            @RequestBody GeminiReviewSubmitRequest request) {
        GeminiReviewSubmitResponse response = geminiPipelineService.submitReview(userId, analysisId, request);
        return ApiResponse.onSuccess(SuccessCode.GEMINI_REVIEW_SUBMIT_SUCCESS, response);
    }

    @PostMapping("/{analysisId}/analyze")
    @Operation(summary = "Gemini 최종 임금 분석", description = "교차 검증을 통과했거나 사용자 검증이 완료된 분석 건에 대해 최종 임금 이상 탐지를 실행합니다.")
    public ApiResponse<GeminiAnalyzeResponse> analyzeVerifiedFields(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long analysisId) {
        GeminiAnalyzeResponse response = geminiPipelineService.analyze(userId, analysisId);
        return ApiResponse.onSuccess(SuccessCode.GEMINI_FINAL_ANALYZE_SUCCESS, response);
    }

    @PostMapping("/analyze")
    @Operation(summary = "문서 목록 분석 (AI)", description = "최대 10개의 문서를 분석할 수 있는 Gemini API입니다.")
    public ApiResponse<Object> analyzeDocuments(
            @AuthenticationPrincipal Long userId,
            @RequestBody DocumentAnalyzeRequest request) {

        String analysisResultJson = geminiService.analyzeDocuments(userId, request);

        return ApiResponse.onSuccess(SuccessCode.DOCUMENT_ANALYZE_SUCCESS, analysisResultJson);
    }

    @Deprecated
    @PostMapping("/document/{documentId}/analyze")
    @Operation(
            summary = "문서 분석 (AI)",
            description = "단일 문서 분석 Gemini API입니다. POST /api/gemini/analyze 사용을 권장합니다.",
            deprecated = true)
    public ApiResponse<Object> analyzeDocument(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long documentId) {

        String analysisResultJson = geminiService.analyzeDocuments(
                userId,
                new DocumentAnalyzeRequest(List.of(documentId))
        );

        return ApiResponse.onSuccess(SuccessCode.DOCUMENT_ANALYZE_SUCCESS, analysisResultJson);
    }
}
