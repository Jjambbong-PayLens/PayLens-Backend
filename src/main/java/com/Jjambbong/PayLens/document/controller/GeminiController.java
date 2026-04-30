package com.Jjambbong.PayLens.document.controller;

import com.Jjambbong.PayLens.document.service.GeminiService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.Jjambbong.PayLens.global.api.ApiResponse;
import com.Jjambbong.PayLens.global.api.SuccessCode;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/gemini")
public class GeminiController {

    private final GeminiService geminiService;

    @PostMapping("/{documentId}/analyze")
    @Operation(summary = "문서 분석 (AI)")
    public ApiResponse<Object> analyzeDocument(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long documentId) {

        String analysisResultJson = geminiService.analyzeDocument(userId, documentId);

        return ApiResponse.onSuccess(SuccessCode.DOCUMENT_ANALYZE_SUCCESS, analysisResultJson);
    }
}
