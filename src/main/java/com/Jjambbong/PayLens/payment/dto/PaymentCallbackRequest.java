package com.Jjambbong.PayLens.payment.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PaymentCallbackRequest {
    private String impUid;       // 포트원 결제 고유 번호
    private String merchantUid;  // 우리 서버 주문 번호
}