package com.Jjambbong.PayLens.user.domain;

public enum LaborApproveStatus {
    NONE,       // 기본 상태 (일반 회원)
    PENDING,    // 노무사 가입/등업 신청 후 승인 대기 중
    APPROVED,   // 어드민 승인 완료
    REJECTED    // 어드민 승인 거절
}