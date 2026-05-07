package com.Jjambbong.PayLens.payment.repository;

import com.Jjambbong.PayLens.payment.domain.Subscription;
import com.Jjambbong.PayLens.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    // "구독 중이고, 결제 예정일이 지금보다 이전(오늘 포함)"인 유저들만 찾기
    List<Subscription> findByActiveTrueAndNextPaymentDateBefore(LocalDateTime dateTime);

    Optional<Subscription> findByUser(User user);
}