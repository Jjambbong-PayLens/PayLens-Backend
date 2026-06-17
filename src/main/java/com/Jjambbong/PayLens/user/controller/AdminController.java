package com.Jjambbong.PayLens.user.controller;

import com.Jjambbong.PayLens.global.api.ApiResponse;
import com.Jjambbong.PayLens.global.api.SuccessCode;
import com.Jjambbong.PayLens.user.domain.User; // 추가된 import
import com.Jjambbong.PayLens.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin", description = "관리자 전용 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;

    @GetMapping("/labor/pending")
    @Operation(summary = "노무사 가입 대기 목록 조회 API", description = "관리자가 가입 승인 대기 중인(PENDING) 유저 목록을 조회합니다.")
    public ApiResponse<List<User>> getPendingLaborApplications() {
        // Service로부터 PENDING 상태인 유저 리스트를 받아옵니다.
        List<User> pendingUsers = userService.getPendingLaborApplications();
        return ApiResponse.onSuccess(SuccessCode.LABOR_PENDING_LIST_SUCCESS, pendingUsers);
    }

    @PostMapping("/labor/{userId}/approve")
    @Operation(summary = "노무사 가입 승인 API", description = "관리자가 대기 중인 유저의 노무사 권한을 승인합니다.")
    public ApiResponse<Object> approveLaborAttorney(@PathVariable Long userId) {

        userService.approveLaborAttorney(userId);
        return ApiResponse.onSuccess(SuccessCode.LABOR_APPROVE_SUCCESS, null);
    }

    @PostMapping("/labor/{userId}/reject")
    @Operation(summary = "노무사 가입 거절 API", description = "관리자가 대기 중인 유저의 노무사 가입을 거절합니다.")
    public ApiResponse<Object> rejectLaborAttorney(@PathVariable Long userId) {

        userService.rejectLaborAttorney(userId);
        return ApiResponse.onSuccess(SuccessCode.LABOR_REJECT_SUCCESS, null);
    }
}