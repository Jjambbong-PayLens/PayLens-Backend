package com.Jjambbong.PayLens.consultation.repository;

import com.Jjambbong.PayLens.consultation.domain.ConsultationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConsultationRequestRepository extends JpaRepository<ConsultationRequest, Long> {
    // 유저가 본인의 과거 상담 신청 내역을 보고 싶어 할 때를 대비한 메서드 미리 작성!
    // List<ConsultationRequest> findByUserId(Long userId);
}