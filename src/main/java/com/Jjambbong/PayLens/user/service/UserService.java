package com.Jjambbong.PayLens.user.service;

import com.Jjambbong.PayLens.user.domain.User;
import com.Jjambbong.PayLens.user.dto.MypageResponseDto;
import com.Jjambbong.PayLens.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
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
}