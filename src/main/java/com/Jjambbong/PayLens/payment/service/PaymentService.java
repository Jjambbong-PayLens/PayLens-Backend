package com.Jjambbong.PayLens.payment.service;

import com.Jjambbong.PayLens.payment.domain.Payment;
import com.Jjambbong.PayLens.payment.domain.PaymentStatus;
import com.Jjambbong.PayLens.payment.domain.Subscription;
import com.Jjambbong.PayLens.payment.repository.PaymentRepository;
import com.Jjambbong.PayLens.payment.repository.SubscriptionRepository;
import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.exception.IamportResponseException;
import com.siot.IamportRestClient.request.AgainPaymentData;
import com.siot.IamportRestClient.response.IamportResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;


import com.Jjambbong.PayLens.payment.domain.Subscription;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository; // 💡 구독 레포지토리 추가
    private final IamportClient iamportClient;

    /**
     * 1. 최초 결제 검증 및 구독(Subscription) 생성 로직
     */
    public void verifyAndCompletePayment(String impUid, String merchantUid) throws IamportResponseException, IOException {

        Payment payment = paymentRepository.findByMerchantUid(merchantUid)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문 번호입니다."));

        IamportResponse<com.siot.IamportRestClient.response.Payment> portOneResponse = iamportClient.paymentByImpUid(impUid);

        if (portOneResponse.getResponse().getAmount().intValue() == payment.getAmount()) {

            String customerUid = portOneResponse.getResponse().getCustomerUid();

            // 결제 상태 업데이트
            payment.completePayment(impUid, customerUid);

            // 💡 [핵심] 구독 정보 생성 또는 업데이트
            saveOrUpdateSubscription(payment, customerUid);

            log.info("최초 결제 검증 및 구독 등록 완료: 주문번호 {}", merchantUid);

        } else {
            throw new RuntimeException("결제 금액이 일치하지 않습니다. 조작된 요청일 수 있습니다.");
        }
    }

    /**
     * 2. 정기 구독 자동 결제 로직 (스케줄러가 호출)
     */
    public void payWithBillingKey(String customerUid, int amount) {
        String newMerchantUid = "paylens_sub_" + UUID.randomUUID().toString().substring(0, 8);

        AgainPaymentData againData = new AgainPaymentData(customerUid, newMerchantUid, BigDecimal.valueOf(amount));
        againData.setName("페이렌즈 월간 구독료");

        try {
            log.info("자동 결제 요청 시작 - 빌링키: {}", customerUid);
            IamportResponse<com.siot.IamportRestClient.response.Payment> response = iamportClient.againPayment(againData);

            if (response.getResponse() != null && response.getResponse().getStatus().equals("paid")) {
                // 💡 자동 결제 성공 시 DB에 새로운 결제 기록 남기기
                savePaymentHistory(response.getResponse(), customerUid);
                log.info("구독 자동 결제 성공! 주문번호: {}", newMerchantUid);
            } else {
                log.error("구독 자동 결제 실패: {}", response.getMessage());
                throw new RuntimeException("자동 결제 실패: " + response.getMessage());
            }
        } catch (Exception e) {
            log.error("자동 결제 요청 중 에러 발생: {}", e.getMessage());
            throw new RuntimeException(e); // 스케줄러에서 트랜잭션 처리를 위해 예외 던짐
        }
    }

    // --- 내부 보조 메서드들 ---

    private void saveOrUpdateSubscription(Payment payment, String customerUid) {
        Subscription subscription = subscriptionRepository.findByUser(payment.getUser())
                .orElse(new Subscription());

        subscription.setUser(payment.getUser());
        subscription.setCustomerUid(customerUid);
        subscription.setAmount(payment.getAmount());
        subscription.setNextPaymentDate(LocalDateTime.now().plusMonths(1)); // 다음 결제는 한 달 뒤
        subscription.setActive(true);

        subscriptionRepository.save(subscription);
    }

    private void savePaymentHistory(com.siot.IamportRestClient.response.Payment response, String customerUid) {
        // 이 부분은 자동 결제 성공 시 'payments' 테이블에 기록을 남기는 로직
        log.info("결제 이력 저장 완료: {}", response.getMerchantUid());
    }


}