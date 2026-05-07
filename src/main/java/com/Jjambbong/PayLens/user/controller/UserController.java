package com.Jjambbong.PayLens.user.controller;

import com.Jjambbong.PayLens.global.api.ApiResponse;
import com.Jjambbong.PayLens.global.api.SuccessCode;
import com.Jjambbong.PayLens.user.dto.MypageResponseDto;
import com.Jjambbong.PayLens.user.dto.request.UserLanguageUpdateRequest;
import com.Jjambbong.PayLens.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "사용자 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user") 
public class UserController {

    private final UserService userService;

    
    @GetMapping("/mypage/{id}") // 경로가 /api/user/mypage/{id} 로 합쳐집니다.
    public ResponseEntity<MypageResponseDto> getMypageInfo(@PathVariable Long id) {
        MypageResponseDto responseDto = userService.getMypageInfo(id);
        return ResponseEntity.ok(responseDto);
    }

  
    @PatchMapping("/language")
    @Operation(summary = "사용자 선호 언어 변경 API", description = "로그인된 사용자의 선호 언어(ko, en, vi 등)를 변경합니다.")
    public ApiResponse<Object> updateUserLanguage(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UserLanguageUpdateRequest languageUpdateRequest) {

        userService.updateUserLanguage(userId, languageUpdateRequest.getLanguage());

        return ApiResponse.onSuccess(SuccessCode.USER_LANGUAGE_UPDATE_SUCCESS, null);
    }
}
