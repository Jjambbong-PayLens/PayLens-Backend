package com.Jjambbong.PayLens.notice.controller;

import com.Jjambbong.PayLens.global.api.ApiResponse;
import com.Jjambbong.PayLens.global.api.SuccessCode;
import com.Jjambbong.PayLens.notice.dto.request.NoticeRequest;
import com.Jjambbong.PayLens.notice.dto.response.NoticeListItemResponse;
import com.Jjambbong.PayLens.notice.dto.response.NoticeResponse;
import com.Jjambbong.PayLens.notice.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notices")
@Tag(name = "Notice", description = "공지사항 API")
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping
    @Operation(summary = "공지사항 목록 조회")
    public ApiResponse<List<NoticeListItemResponse>> getNotices() {
        List<NoticeListItemResponse> response = noticeService.getNotices();
        return ApiResponse.onSuccess(SuccessCode.NOTICE_LIST_GET_SUCCESS, response);
    }

    @GetMapping("/{noticeId}")
    @Operation(summary = "공지사항 상세 조회")
    public ApiResponse<NoticeResponse> getNotice(@PathVariable Long noticeId) {
        NoticeResponse response = noticeService.getNotice(noticeId);
        return ApiResponse.onSuccess(SuccessCode.NOTICE_GET_SUCCESS, response);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "공지사항 작성 (관리자용)", description = "ADMIN 권한을 가진 사용자만 작성할 수 있습니다. Authorization 헤더에 Bearer accessToken이 필요합니다.")
    public ApiResponse<Long> createNotice(@Valid @RequestBody NoticeRequest request) {
        Long noticeId = noticeService.createNotice(request).getId();
        return ApiResponse.onSuccess(SuccessCode.NOTICE_CREATE_SUCCESS, noticeId);
    }

    @PutMapping("/{noticeId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "공지사항 수정 (관리자용)", description = "ADMIN 권한을 가진 사용자만 수정할 수 있습니다. Authorization 헤더에 Bearer accessToken이 필요합니다.")
    public ApiResponse<Long> updateNotice(@PathVariable Long noticeId, @Valid @RequestBody NoticeRequest request) {
        Long updatedNoticeId = noticeService.updateNotice(noticeId, request).getId();
        return ApiResponse.onSuccess(SuccessCode.NOTICE_UPDATE_SUCCESS, updatedNoticeId);
    }

    @DeleteMapping("/{noticeId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "공지사항 삭제 (관리자용)", description = "ADMIN 권한을 가진 사용자만 삭제할 수 있습니다. Authorization 헤더에 Bearer accessToken이 필요합니다.")
    public ApiResponse<Void> deleteNotice(@PathVariable Long noticeId) {
        noticeService.deleteNotice(noticeId);
        return ApiResponse.onSuccess(SuccessCode.NOTICE_DELETE_SUCCESS, null);
    }
}