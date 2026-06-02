package com.Jjambbong.PayLens.consultation.repository;

import com.Jjambbong.PayLens.consultation.domain.LaborConsultant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LaborConsultantRepository extends JpaRepository<LaborConsultant, Long> {
    // JpaRepository를 상속받기만 하면, 기본 CRUD(저장, 단건 조회, 전체 조회 등)가 자동으로 생성됩니다!
}