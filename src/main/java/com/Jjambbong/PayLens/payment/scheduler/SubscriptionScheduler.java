package com.Jjambbong.PayLens.payment.scheduler;

import com.Jjambbong.PayLens.payment.domain.Subscription;
import com.Jjambbong.PayLens.payment.repository.SubscriptionRepository;
import com.Jjambbong.PayLens.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionScheduler {

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentService paymentService;

    @Transactional
    @Scheduled(cron = "0 0 10 * * ?") // 매일 오전 10시
    public void processSubscriptions() {
        log.info("[정기 결제 시스템] 오늘자 결제 대상 확인 중...");

        // 1. 오늘 결제해야 하는 활성 구독자들만 가져옴
        List<Subscription> targets = subscriptionRepository.findByActiveTrueAndNextPaymentDateBefore(LocalDateTime.now());

        for (Subscription sub : targets) {
            try {
                log.info("결제 시도: 유저 {}", sub.getUser().getEmail());

                // 2. 실제 결제 요청
                paymentService.payWithBillingKey(sub.getCustomerUid(), sub.getAmount());

                // 3. 결제 성공 시 다음 결제일을 한 달 뒤로 업데이트
                sub.renewSubscription();
                log.info("결제 성공 및 차기 예정일 갱신: {}", sub.getNextPaymentDate());

            } catch (Exception e) {
                log.error("결제 실패 (유저 {}): {}", sub.getUser().getEmail(), e.getMessage());
            }
        }
    }
}