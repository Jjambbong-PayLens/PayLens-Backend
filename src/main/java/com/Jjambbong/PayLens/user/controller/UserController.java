package com.Jjambbong.PayLens.user.controller;

import com.Jjambbong.PayLens.global.api.ApiResponse;
import com.Jjambbong.PayLens.global.api.SuccessCode;
import com.Jjambbong.PayLens.user.dto.request.UserLanguageUpdateRequest;
import com.Jjambbong.PayLens.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "사용자 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    @PatchMapping("/language")
    @Operation(summary = "사용자 선호 언어 변경 API", description = "로그인된 사용자의 선호 언어(ko, en, vi 등)를 변경합니다.")
    public ApiResponse<Object> updateUserLanguage(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UserLanguageUpdateRequest languageUpdateRequest) {

        userService.updateUserLanguage(userId, languageUpdateRequest.getLanguage());

        return ApiResponse.onSuccess(SuccessCode.USER_LANGUAGE_UPDATE_SUCCESS, null);
    }

    // 노무사 등업 신청
    @PostMapping("/labor/apply")
    @Operation(summary = "노무사 등업 신청 API", description = "증빙 서류 업로드 후 노무사 승인 대기 상태로 전환합니다.")
    public ApiResponse<Object> applyForLaborAttorney(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {

        // 1. 서비스 로직 호출 (상태를 PENDING으로 변경)
        userService.applyForLaborAttorney(userId);

        // 2. 성공 응답 반환
        return ApiResponse.onSuccess(SuccessCode.LABOR_APPLY_SUCCESS, null);
    }
}