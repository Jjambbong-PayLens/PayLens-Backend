package com.Jjambbong.PayLens.document.controller;

import com.Jjambbong.PayLens.document.dto.request.DocumentAnalyzeRequest;
import com.Jjambbong.PayLens.document.service.GeminiService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.Jjambbong.PayLens.global.api.ApiResponse;
import com.Jjambbong.PayLens.global.api.SuccessCode;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/gemini")
public class GeminiController {

    private final GeminiService geminiService;

    @PostMapping("/analyze")
    @Operation(summary = "문서 목록 분석 (AI)", description = "최대 10개의 문서를 분석할 수 있는 Gemini API입니다.")
    public ApiResponse<Object> analyzeDocuments(
            @AuthenticationPrincipal Long userId,
            @RequestBody DocumentAnalyzeRequest request) {

        String analysisResultJson = geminiService.analyzeDocuments(userId, request);

        return ApiResponse.onSuccess(SuccessCode.DOCUMENT_ANALYZE_SUCCESS, analysisResultJson);
    }

    @Deprecated
    @PostMapping("/{documentId}/analyze")
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
