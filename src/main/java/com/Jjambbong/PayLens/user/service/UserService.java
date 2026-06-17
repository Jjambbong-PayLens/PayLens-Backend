package com.Jjambbong.PayLens.user.service;

import com.Jjambbong.PayLens.global.api.ErrorCode;
import com.Jjambbong.PayLens.global.exception.GeneralException;
import com.Jjambbong.PayLens.user.domain.LaborApproveStatus;
import com.Jjambbong.PayLens.user.domain.Language;
import com.Jjambbong.PayLens.user.domain.User;
import com.Jjambbong.PayLens.user.domain.UserRole;
import com.Jjambbong.PayLens.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List; // 추가된 import

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public void updateUserLanguage(Long userId, Language language) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        user.updatePreferredLanguage(language);
    }

    // 노무사 등업 신청
    public void applyForLaborAttorney(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        // 1. 이미 승인 대기 중이거나 완료된 유저인지 검사 (방어 로직)
        if (user.getLaborApproveStatus() == LaborApproveStatus.PENDING) {
            throw new GeneralException(ErrorCode.PENDING_ALREADY_EXISTS);
        }
        if (user.getLaborApproveStatus() == LaborApproveStatus.APPROVED) {
            throw new GeneralException(ErrorCode.ALREADY_APPROVED_LABOR);
        }

        // 2. 유저의 상태를 PENDING(승인 대기중)으로 업데이트
        user.updateLaborApproveStatus(LaborApproveStatus.PENDING);
    }

    // 어드민: 노무사 승인
    public void approveLaborAttorney(Long targetUserId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        // 대기 중(PENDING)인 유저만 승인 가능하도록 함
        if (user.getLaborApproveStatus() != LaborApproveStatus.PENDING) {
            throw new GeneralException(ErrorCode.BAD_REQUEST);
        }

        // 상태를 '승인 완료'로 바꾸고, 권한(Role)도 '노무사'로 업그레이드
        user.updateLaborApproveStatus(LaborApproveStatus.APPROVED);
        user.updateRole(UserRole.LABOR_ATTORNEY);
    }

    // 어드민: 노무사 거절
    public void rejectLaborAttorney(Long targetUserId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        // 대기 중(PENDING)인 유저만 거절 가능하도록 방어
        if (user.getLaborApproveStatus() != LaborApproveStatus.PENDING) {
            throw new GeneralException(ErrorCode.BAD_REQUEST);
        }

        // 상태를 '거절됨'으로 변경
        user.updateLaborApproveStatus(LaborApproveStatus.REJECTED);
    }

    // 어드민: 노무사 가입 대기 목록 조회
    @Transactional(readOnly = true)
    public List<User> getPendingLaborApplications() {
        // Repository를 통해 PENDING 상태인 유저들만 리스트 형태로 반환
        return userRepository.findByLaborApproveStatus(LaborApproveStatus.PENDING);
    }
}