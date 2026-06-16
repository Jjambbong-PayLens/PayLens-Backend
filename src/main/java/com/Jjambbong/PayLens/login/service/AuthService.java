package com.Jjambbong.PayLens.login.service;

import com.Jjambbong.PayLens.user.domain.Language;
import com.Jjambbong.PayLens.user.domain.LaborApproveStatus;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import com.Jjambbong.PayLens.global.api.ErrorCode;
import com.Jjambbong.PayLens.global.exception.GeneralException;
import com.Jjambbong.PayLens.login.client.KakaoClient;
import com.Jjambbong.PayLens.login.client.GoogleClient;
import com.Jjambbong.PayLens.login.domain.RefreshToken;
import com.Jjambbong.PayLens.login.dto.response.AuthResponse;
import com.Jjambbong.PayLens.login.jwt.JwtProvider;
import com.Jjambbong.PayLens.login.repository.RefreshTokenRepository;
import com.Jjambbong.PayLens.user.domain.User;
import com.Jjambbong.PayLens.user.domain.UserRole;
import com.Jjambbong.PayLens.user.domain.UserStatus;
import com.Jjambbong.PayLens.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final KakaoClient kakaoClient;
    private final GoogleClient googleClient;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;

    public AuthResponse handleKakaoCode(String code) {

        // code -> 카카오 access_token
        String kakaoAccessToken = kakaoClient.getAccessToken(code);

        // access token -> kakao user info
        JsonNode kakaoUserInfo = kakaoClient.getUserInfo(kakaoAccessToken);

        String providerId = kakaoUserInfo.get("id").asText();
        String username = kakaoUserInfo
                .path("kakao_account")
                .path("profile")
                .path("nickname")
                .asText("유저");
        String email = kakaoUserInfo
                .path("kakao_account")
                .path("email")
                .asText(providerId + "@kakao.com");

        return processUserLogin(providerId, username, email);
    }

    public AuthResponse handleGoogleCode(String code) {

        // code -> 구글 access_token
        String googleAccessToken = googleClient.getAccessToken(code);

        // access token -> 구글 user info
        JsonNode googleUserInfo = googleClient.getUserInfo(googleAccessToken);

        // 구글은 보통 'sub' 또는 'id' 로 고유 식별자를 반환합니다.
        String idNode = googleUserInfo.has("sub") ? googleUserInfo.get("sub").asText() : googleUserInfo.get("id").asText();
        String providerId = idNode;
        String username = googleUserInfo.has("name") ? googleUserInfo.get("name").asText() : "유저";
        String email = googleUserInfo.has("email") ? googleUserInfo.get("email").asText() : providerId + "@google.com";

        return processUserLogin(providerId, username, email);
    }

    private AuthResponse processUserLogin(String providerId, String username, String email) {

        // 1. 먼저 providerId로 유저를 찾습니다.
        Optional<User> existingUser = userRepository.findByProviderId(providerId);
        User user;

        if (existingUser.isPresent()) {
            // 2-a. 해당 providerId로 이미 가입된 유저가 있으면 그대로 사용
            user = existingUser.get();

            // (참고) 만약 어드민에게 노무사 승인 거절(REJECTED) 당한 유저의 로그인을 막고 싶다면
            // 이곳에 if (user.getLaborApproveStatus() == LaborApproveStatus.REJECTED) { throw ... } 를 추가하시면 됩니다.

        } else {
            // 2-b. providerId로 가입된 내역이 없을 경우, 이메일 중복 검사를 합니다.
            if (userRepository.findByEmail(email).isPresent()) {
                // 이메일이 이미 존재하면 (즉, 다른 소셜 계정으로 이미 가입한 경우) 예외를 발생시킵니다.
                throw new GeneralException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
            // 3. 이메일도 중복되지 않으면 새로운 유저로 생성
            user = createUser(providerId, username, email);
        }

        // JWT 발급
        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());
        Long refreshTokenExpiration = jwtProvider.getRefreshTokenExpiration();

        // RefreshToken 저장/업데이트
        saveOrUpdateRefreshToken(user, refreshToken, refreshTokenExpiration);

        // 응답 (유저 정보 + accessToken)
        return AuthResponse.from(user, accessToken);
    }

    private User createUser(String providerId, String username, String email) {
        User user = User.builder()
                .providerId(providerId)
                .username(username)
                .email(email)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .preferredLanguage(Language.KO)
                .laborApproveStatus(LaborApproveStatus.NONE) //최초 가입 시 노무사 승인 상태 기본값
                .build();

        return userRepository.save(user);
    }

    private void saveOrUpdateRefreshToken(User user, String token, Long expiration) {
        refreshTokenRepository.findByUser(user)
                .ifPresentOrElse(
                        existing -> existing.updateToken(token, expiration),
                        () -> refreshTokenRepository.save(
                                RefreshToken.builder()
                                        .user(user)
                                        .refreshToken(token)
                                        .refreshTokenExpiration(expiration)
                                        .build()
                        )
                );
    }

    public String reissueAccessToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer == null || !bearer.startsWith("Bearer ")) {
            throw new GeneralException(ErrorCode.TOKEN_INVALID);
        }

        String accessToken = bearer.substring(7);

        // accessToken이 만료돼도 Claims는 뽑을 수 있어야 재발급이 가능함
        Long userId;
        try {
            Claims claims = jwtProvider.parseClaims(accessToken); // 만료면 ExpiredJwtException에서 claims 반환
            String subject = claims.getSubject();
            if (subject == null || subject.isBlank()) {
                throw new GeneralException(ErrorCode.TOKEN_INVALID);
            }
            userId = Long.parseLong(subject);
        } catch (Exception e) {
            throw new GeneralException(ErrorCode.TOKEN_INVALID);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        RefreshToken saved = refreshTokenRepository.findByUser(user)
                .orElseThrow(() -> new GeneralException(ErrorCode.WRONG_REFRESH_TOKEN));

        try {
            jwtProvider.validate(saved.getRefreshToken());
        } catch (Exception e) {
            refreshTokenRepository.delete(saved);
            throw new GeneralException(ErrorCode.TOKEN_EXPIRED);
        }

        return jwtProvider.createAccessToken(userId);
    }

    // accessToken 내 userId 추출 후 refreshToken 삭제
    public void logout(HttpServletRequest request) {

        User user = validateUser(request);

        RefreshToken saved = refreshTokenRepository.findByUser(user)
                .orElseThrow(() -> new GeneralException(ErrorCode.WRONG_REFRESH_TOKEN));

        refreshTokenRepository.delete(saved);
    }

    // 유효한 accessToken 으로 인증 후 회원탈퇴
    public void withdraw(HttpServletRequest request) {

        User user = validateUser(request);

        refreshTokenRepository.deleteByUser(user);

        userRepository.delete(user);
    }

    public User validateUser(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer == null || !bearer.startsWith("Bearer ")) {
            throw new GeneralException(ErrorCode.TOKEN_INVALID);
        }

        String accessToken = bearer.substring(7);

        try {
            jwtProvider.validate(accessToken);
        } catch (Exception e) {
            throw new GeneralException(ErrorCode.TOKEN_INVALID);
        }

        Long userId;

        try {
            userId = jwtProvider.getUserId(accessToken);
        } catch (NumberFormatException e) {
            throw new GeneralException(ErrorCode.TOKEN_INVALID);
        }

        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));
    }
}