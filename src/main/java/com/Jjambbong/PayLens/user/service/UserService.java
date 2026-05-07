package com.Jjambbong.PayLens.user.service;

import com.Jjambbong.PayLens.global.api.ErrorCode;
import com.Jjambbong.PayLens.global.exception.GeneralException;
import com.Jjambbong.PayLens.user.domain.Language;
import com.Jjambbong.PayLens.user.domain.User;
import com.Jjambbong.PayLens.user.dto.MypageResponseDto;
import com.Jjambbong.PayLens.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional 
public class UserService {

    private final UserRepository userRepository;


    public MypageResponseDto getMypageInfo(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다. id: " + id));

        return new MypageResponseDto(
                user.getUsername(),
                user.getEmail(),
                user.getPreferredLanguage()
        );
    }

    
    public void updateUserLanguage(Long userId, Language language) {
        // userId로 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        // 사용자의 선호 언어 정보 업데이트
        user.updatePreferredLanguage(language);
    }
}
