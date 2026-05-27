package com.Jjambbong.PayLens.payment.domain;

import com.Jjambbong.PayLens.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter // 상태 변경을 위해 Setter 추가
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String customerUid; // 빌링키
    private Integer amount;     // 정기 결제 금액
    private LocalDateTime nextPaymentDate; // 다음 결제 예정일
    private boolean active;     // 구독 활성화 여부

    // 결제 성공 시 다음 결제일을 한 달 뒤로 미루는 메서드
    public void renewSubscription() {
        this.nextPaymentDate = this.nextPaymentDate.plusMonths(1);
    }
}