package com.Jjambbong.PayLens.payment.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PaymentPrepareRequest {
    private Long userId;     // 결제하는 유저 ID
    private Integer amount;  // 결제 예정 금액
}
