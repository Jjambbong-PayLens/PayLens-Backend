package com.Jjambbong.PayLens.consultation.controller;

import com.Jjambbong.PayLens.consultation.service.ConsultationService;
import com.Jjambbong.PayLens.global.api.ApiResponse;
import com.Jjambbong.PayLens.global.api.SuccessCode;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/consultation")
@RequiredArgsConstructor
public class ConsultationController {

    private final ConsultationService consultationService;

    /**
     * 노무사 상담 매칭 신청 및 리포트 파일 전송 API
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<String> applyConsultation(
            @RequestParam("consultantId") Long consultantId,
            @RequestPart("file") MultipartFile file,
            Authentication authentication // 시큐리티 필터를 통과한 유저의 인증 객체
    ) {
        // 1. JWT 토큰에서 현재 로그인한 유저의 식별자(ID) 추출
        String currentUserIdString = authentication.getName();
        Long realUserId = Long.parseLong(currentUserIdString);

        // 2. 서비스 로직 호출 (이메일 발송 및 DB 상태 저장)
        consultationService.requestConsultation(realUserId, consultantId, file);

        // 3. 팀 공통 응답 포맷(ApiResponse)으로 반환
        return ApiResponse.onSuccess(
                SuccessCode.OK,
                "노무사 매칭 신청 및 리포트 발송이 완료되었습니다."
        );
    }
}