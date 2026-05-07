package com.Jjambbong.PayLens.payment.domain;

import com.Jjambbong.PayLens.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 누가 결제했는지 (다대일 관계)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 1. 우리 서버의 주문 번호
    @Column(nullable = false, unique = true)
    private String merchantUid;

    // 2. 포트원의 결제 고유 번호 (결제 완료 후 세팅됨)
    @Column(unique = true)
    private String impUid;

    // 결제 금액
    @Column(nullable = false)
    private Integer amount;

    // 결제 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    // 빌링키 (정기 구독 결제를 위해 포트원에서 발급받는 키)
    @Column
    private String customerUid;

    @Builder
    public Payment(User user, String merchantUid, Integer amount, PaymentStatus status, String customerUid) {
        this.user = user;
        this.merchantUid = merchantUid;
        this.amount = amount;
        this.status = status;
        this.customerUid = customerUid;
    }

    // 결제 성공 후 포트원 영수증 번호를 업데이트하는 메서드
    public void completePayment(String impUid, String customerUid) {
        this.impUid = impUid;
        this.status = PaymentStatus.PAID;
        this.customerUid = customerUid; // 👈 포트원이 준 빌링키를 내 DB에 저장!
    }
}