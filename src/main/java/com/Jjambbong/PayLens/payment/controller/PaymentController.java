package com.Jjambbong.PayLens.payment.controller;

import com.Jjambbong.PayLens.global.api.ApiResponse;
import com.Jjambbong.PayLens.global.api.SuccessCode;
import com.Jjambbong.PayLens.global.api.ErrorCode;
import com.Jjambbong.PayLens.payment.dto.PaymentCallbackRequest;
import com.Jjambbong.PayLens.payment.dto.PaymentPrepareRequest;
import com.Jjambbong.PayLens.payment.dto.PaymentPrepareResponse;
import com.Jjambbong.PayLens.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 0. 프론트엔드가 결제창 띄우기 전, 서버에 주문번호 선발급 요청 (사전 검증용)
     */
    @PostMapping("/prepare")
    public ApiResponse<PaymentPrepareResponse> preparePayment(@RequestBody PaymentPrepareRequest request) {
        try {
            log.info("결제 준비 요청 - 유저 ID: {}, 금액: {}", request.getUserId(), request.getAmount());
            PaymentPrepareResponse response = paymentService.preparePayment(request);

            // 성공 시
            return ApiResponse.onSuccess(SuccessCode.OK, response);

        } catch (IllegalArgumentException e) {
            log.warn("결제 준비 실패 (보안/값 오류): {}", e.getMessage());

            // 실패 시
            return ApiResponse.onFailure(ErrorCode.BAD_REQUEST, null);
        }
    }

    /**
     * 1. 프론트엔드로부터 결제 완료 보고를 받고 검증을 진행
     */
    @PostMapping("/verify")
    public ApiResponse<String> verifyPayment(@RequestBody PaymentCallbackRequest request) {
        try {
            paymentService.verifyAndCompletePayment(request.getImpUid(), request.getMerchantUid());
            log.info("결제 검증 완료: 주문번호 {}", request.getMerchantUid());

            return ApiResponse.onSuccess(SuccessCode.OK, "결제가 성공적으로 완료되었습니다.");

        } catch (IllegalArgumentException e) {
            log.error("보안 검증 실패: {}", e.getMessage());
            // 권한 거부 관련 에러
            return ApiResponse.onFailure(ErrorCode.BAD_REQUEST, e.getMessage());

        } catch (RuntimeException e) {
            log.error("결제 처리 중 서버 내부 오류: {}", e.getMessage());
            // 서버 내부 오류
            return ApiResponse.onFailure(ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}