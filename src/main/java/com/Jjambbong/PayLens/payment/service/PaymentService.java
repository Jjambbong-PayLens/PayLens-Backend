package com.Jjambbong.PayLens.payment.service;

import com.Jjambbong.PayLens.payment.domain.Payment;
import com.Jjambbong.PayLens.payment.domain.PaymentStatus;
import com.Jjambbong.PayLens.payment.domain.Subscription;
import com.Jjambbong.PayLens.payment.dto.PaymentPrepareRequest;
import com.Jjambbong.PayLens.payment.dto.PaymentPrepareResponse;
import com.Jjambbong.PayLens.payment.repository.PaymentRepository;
import com.Jjambbong.PayLens.payment.repository.SubscriptionRepository;
import com.Jjambbong.PayLens.user.domain.User;
import com.Jjambbong.PayLens.user.repository.UserRepository;
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
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository; // 💡 사전 주문 생성을 위해 유저 레포지토리 추가
    private final IamportClient iamportClient;

    /**
     * 0. 프론트엔드가 결제창 띄우기 전, 서버에 주문번호 선발급 및 결제 데이터 생성
     */
    public PaymentPrepareResponse preparePayment(PaymentPrepareRequest request) {

        // 결제 금액 위변조 방어 로직 (4900원 고정)
        if (request.getAmount() == null || request.getAmount() != 4900) {
            log.error("비정상적인 결제 금액 요청 감지: {}", request.getAmount());
            throw new IllegalArgumentException("잘못된 결제 금액입니다. (정상가: 4900원)");
        }

        // 고유한 주문번호 생성 (예: ORD-20240512-랜덤)
        String merchantUid = "ORD-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 8);

        // 결제할 유저 조회
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 결제 대기 데이터 생성 (READY 상태)
        Payment payment = Payment.builder()
                .user(user)
                .merchantUid(merchantUid)
                .amount(request.getAmount())
                .status(PaymentStatus.READY) // 결제 전 상태
                .build();

        paymentRepository.save(payment);
        log.info("사전 결제 데이터 생성 완료 - 유저: {}, 주문번호: {}, 금액: {}", user.getEmail(), merchantUid, request.getAmount());

        return new PaymentPrepareResponse(merchantUid);
    }

    /**
     * 1. 최초 결제 검증 및 구독(Subscription) 생성 로직
     */
    public void verifyAndCompletePayment(String impUid, String merchantUid) {

        // DB에 미리 생성된 주문이 있는지 확인 (사전 검증)
        Payment payment = paymentRepository.findByMerchantUid(merchantUid)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문 번호입니다."));

        IamportResponse<com.siot.IamportRestClient.response.Payment> portOneResponse;

        // 외부 API 통신 에러를 서비스 단에서 직접 잡음 (보안/캡슐화)
        try {
            portOneResponse = iamportClient.paymentByImpUid(impUid);
        } catch (IamportResponseException | IOException e) {
            log.error("포트원 서버 통신 에러 (결제 검증 실패): {}", e.getMessage());
            // 외부 에러의 상세 내용을 숨기고, 안전한 범용 에러로 변환하여 던짐
            throw new RuntimeException("결제 검증 중 서버 통신 오류가 발생했습니다.");
        }

        if (portOneResponse.getResponse() == null) {
            throw new IllegalArgumentException("포트원에 존재하지 않는 결제 내역입니다.");
        }

        // 금액 위변조 검증
        if (portOneResponse.getResponse().getAmount().intValue() == payment.getAmount()) {
            String customerUid = portOneResponse.getResponse().getCustomerUid();

            // 결제 상태 업데이트 (PAID 등)
            payment.completePayment(impUid, customerUid);

            // 구독 정보 생성 또는 업데이트
            saveOrUpdateSubscription(payment, customerUid);

            log.info("최초 결제 검증 및 구독 등록 완료: 주문번호 {}", merchantUid);

        } else {
            log.error("결제 금액 조작 의심 - DB: {}, 포트원: {}", payment.getAmount(), portOneResponse.getResponse().getAmount());
            throw new IllegalArgumentException("결제 금액이 일치하지 않습니다. 조작된 요청일 수 있습니다.");
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
                savePaymentHistory(response.getResponse(), customerUid);
                log.info("구독 자동 결제 성공! 주문번호: {}", newMerchantUid);
            } else {
                log.error("구독 자동 결제 실패: {}", response.getMessage());
                throw new RuntimeException("자동 결제 실패: " + response.getMessage());
            }
        } catch (Exception e) {
            log.error("자동 결제 요청 중 에러 발생: {}", e.getMessage());
            throw new RuntimeException(e);
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
        // 1. 빌링키(customerUid)로 구독 중인 유저 정보 찾기
        Subscription subscription = subscriptionRepository.findByCustomerUid(customerUid)
                .orElseThrow(() -> new RuntimeException("해당 빌링키에 대한 구독 정보를 찾을 수 없습니다."));

        // 2. 새로운 결제 내역(Payment) 엔티티 생성
        Payment paymentHistory = Payment.builder()
                .user(subscription.getUser())
                .merchantUid(response.getMerchantUid())
                .amount(response.getAmount().intValue())
                .status(PaymentStatus.READY) // 초기 상태
                .build();

        // 3. 결제 완료 상태(PAID) 및 영수증 번호(impUid) 업데이트
        paymentHistory.completePayment(response.getImpUid(), customerUid);

        // 4. DB에 저장
        paymentRepository.save(paymentHistory);
        log.info("결제 이력 DB 저장 완료: 주문번호 {}", response.getMerchantUid());
    }
}