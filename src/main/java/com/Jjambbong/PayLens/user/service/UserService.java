package com.Jjambbong.PayLens.user.service;

import com.Jjambbong.PayLens.global.api.ErrorCode;
import com.Jjambbong.PayLens.global.exception.GeneralException;
import com.Jjambbong.PayLens.user.domain.LaborApproveStatus;
import com.Jjambbong.PayLens.user.domain.Language;
import com.Jjambbong.PayLens.user.domain.User;
import com.Jjambbong.PayLens.user.domain.UserRole; // 👉 노무사 권한 부여를 위해 UserRole import 추가!
import com.Jjambbong.PayLens.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}