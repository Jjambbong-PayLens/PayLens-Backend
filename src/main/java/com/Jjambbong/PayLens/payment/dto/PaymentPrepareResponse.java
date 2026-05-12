package com.Jjambbong.PayLens.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PaymentPrepareResponse {
    private String merchantUid; // 서버가 생성한 고유 주문번호
}