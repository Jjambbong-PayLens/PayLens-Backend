package com.Jjambbong.PayLens.document.controller;

import com.Jjambbong.PayLens.document.dto.request.DocumentUploadUrlRequest;
import com.Jjambbong.PayLens.document.dto.response.DocumentCompleteResponse;
import com.Jjambbong.PayLens.document.dto.response.DocumentUploadUrlResponse;
import com.Jjambbong.PayLens.document.service.DocumentService;
import com.Jjambbong.PayLens.global.api.ApiResponse;
import com.Jjambbong.PayLens.global.api.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/documents")
@Tag(name = "document-controller", description = "문서 업로드 관련 API")
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload-url")
    @Operation(summary = "문서 업로드 URL 생성", description = "클라이언트에게 문서 업로드 URL을 생성하는 메서드입니다.")
    public ApiResponse<DocumentUploadUrlResponse> createUploadUrl(
            @AuthenticationPrincipal Long userId,
            @RequestBody DocumentUploadUrlRequest request) {
        DocumentUploadUrlResponse response = documentService.createUploadUrl(userId, request);
        return ApiResponse.onSuccess(SuccessCode.DOCUMENT_UPLOAD_URL_CREATED, response);
    }

    @PostMapping("/{documentId}/complete")
    @Operation(summary = "문서 업로드 완료", description = "문서 업로드가 완료되었는지 확인하는 메서드입니다.")
    public ApiResponse<DocumentCompleteResponse> completeUpload(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long documentId) {
        DocumentCompleteResponse response = documentService.completeUpload(userId, documentId);
        return ApiResponse.onSuccess(SuccessCode.DOCUMENT_UPLOAD_COMPLETE_SUCCESS, response);
    }
}

