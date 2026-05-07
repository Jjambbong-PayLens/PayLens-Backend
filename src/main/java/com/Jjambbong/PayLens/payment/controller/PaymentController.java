package com.Jjambbong.PayLens.payment.controller;

import com.Jjambbong.PayLens.payment.dto.PaymentCallbackRequest;
import com.Jjambbong.PayLens.payment.service.PaymentService;
import com.siot.IamportRestClient.exception.IamportResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 프론트엔드로부터 결제 완료 보고를 받고 검증을 진행
     */
    @PostMapping("/verify")
    public ResponseEntity<String> verifyPayment(@RequestBody PaymentCallbackRequest request) {
        try {
            // 영수증 번호와 주문 번호로 검증 및 완료 처리 진행
            paymentService.verifyAndCompletePayment(request.getImpUid(), request.getMerchantUid());

            log.info("결제 검증 완료: 주문번호 {}", request.getMerchantUid());
            return ResponseEntity.ok("결제가 성공적으로 완료되었습니다.");

        } catch (IamportResponseException | IOException e) {
            log.error("결제 검증 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest().body("결제 검증 실패: " + e.getMessage());
        } catch (RuntimeException e) {
            log.error("보안 검증 실패: {}", e.getMessage());
            return ResponseEntity.status(403).body("보안 경고: " + e.getMessage());
        }
    }


}