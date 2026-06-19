package com.Jjambbong.PayLens.user.repository;

import com.Jjambbong.PayLens.user.domain.LaborApproveStatus;
import com.Jjambbong.PayLens.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByProviderId(String providerId);
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email); // 이메일로 유저 찾기 추가
    List<User> findByLaborApproveStatus(LaborApproveStatus status); //노무사 승인 상태(LaborApproveStatus)를 기준으로 유저 목록을 찾아오는 메서드
}
