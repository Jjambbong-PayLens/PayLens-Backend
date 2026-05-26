package com.Jjambbong.PayLens.document.controller;

import com.Jjambbong.PayLens.document.dto.request.DocumentCompleteRequest;
import com.Jjambbong.PayLens.document.dto.request.DocumentDeleteRequest;
import com.Jjambbong.PayLens.document.dto.request.DocumentUploadUrlRequest;
import com.Jjambbong.PayLens.document.dto.request.DocumentUploadUrlsRequest;
import com.Jjambbong.PayLens.document.dto.response.*;
import com.Jjambbong.PayLens.document.service.DocumentService;
import com.Jjambbong.PayLens.global.api.ApiResponse;
import com.Jjambbong.PayLens.global.api.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping
    @Operation(summary = "업로드 문서 목록 조회", description = "사용자가 업로드 완료한 문서 목록을 조회하는 메서드입니다.")
    public ApiResponse<DocumentListResponse> getUploadedDocuments(
            @AuthenticationPrincipal Long userId) {
        DocumentListResponse response = documentService.getUploadedDocuments(userId);
        return ApiResponse.onSuccess(SuccessCode.DOCUMENT_LIST_GET_SUCCESS, response);
    }

    @PostMapping("/upload-url")
    @Deprecated
    @Operation(
            summary = "[Deprecated] 문서 업로드 URL 생성",
            description = "단일 문서 업로드 URL 생성 API입니다. POST /api/documents/upload-urls 사용을 권장합니다.",
            deprecated = true
    )
    public ApiResponse<DocumentUploadUrlResponse> createUploadUrl(
            @AuthenticationPrincipal Long userId,
            @RequestBody DocumentUploadUrlRequest request) {
        DocumentUploadUrlResponse response = documentService.createUploadUrl(userId, request);
        return ApiResponse.onSuccess(SuccessCode.DOCUMENT_UPLOAD_URL_CREATED, response);
    }

    @PostMapping("/upload-urls")
    @Operation(summary = "문서 업로드 URL 목록 생성", description = "클라이언트에게 최대 10개의 문서 업로드 URL을 생성하는 메서드입니다.")
    public ApiResponse<DocumentUploadUrlsResponse> createUploadUrls(
            @AuthenticationPrincipal Long userId,
            @RequestBody DocumentUploadUrlsRequest request) {
        DocumentUploadUrlsResponse response = documentService.createUploadUrls(userId, request);
        return ApiResponse.onSuccess(SuccessCode.DOCUMENT_UPLOAD_URLS_CREATED, response);
    }

    @PostMapping("/complete")
    @Operation(summary = "문서 업로드 목록 완료", description = "최대 10개의 문서 업로드 완료 여부를 확인하는 메서드입니다.")
    public ApiResponse<DocumentCompleteListResponse> completeUploads(
            @AuthenticationPrincipal Long userId,
            @RequestBody DocumentCompleteRequest request) {
        DocumentCompleteListResponse response = documentService.completeUploads(userId, request);
        return ApiResponse.onSuccess(SuccessCode.DOCUMENT_UPLOAD_COMPLETE_LIST_SUCCESS, response);
    }

    @PostMapping("/{documentId}/complete")
    @Deprecated
    @Operation(
            summary = "[Deprecated] 문서 업로드 완료",
            description = "단일 문서 업로드 완료 API입니다. POST /api/documents/complete 사용을 권장합니다.",
            deprecated = true
    )
    public ApiResponse<DocumentCompleteResponse> completeUpload(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long documentId) {
        DocumentCompleteResponse response = documentService.completeUpload(userId, documentId);
        return ApiResponse.onSuccess(SuccessCode.DOCUMENT_UPLOAD_COMPLETE_SUCCESS, response);
    }

    @DeleteMapping
    @Operation(summary = "문서 삭제", description = "사용자가 업로드한 문서를 S3와 DB에서 삭제하는 메서드입니다.")
    public ApiResponse<DocumentDeleteListResponse> deleteDocuments(
            @AuthenticationPrincipal Long userId,
            @RequestBody DocumentDeleteRequest request) {
        DocumentDeleteListResponse response = documentService.deleteDocuments(userId, request);
        return ApiResponse.onSuccess(SuccessCode.DOCUMENT_DELETE_SUCCESS, response);
    }
}
