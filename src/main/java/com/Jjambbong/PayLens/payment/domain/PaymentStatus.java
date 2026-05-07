package com.Jjambbong.PayLens.payment.domain;

public enum PaymentStatus {
    READY,      // 결제 요청 전 (준비 상태)
    PAID,       // 결제 완료
    FAILED,     // 결제 실패
    CANCELLED   // 결제 취소 (환불)
}