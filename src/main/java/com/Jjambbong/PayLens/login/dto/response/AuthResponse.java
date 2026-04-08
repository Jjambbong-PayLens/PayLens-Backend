package com.Jjambbong.PayLens.login.dto.response;

import com.Jjambbong.PayLens.user.domain.User;
import com.Jjambbong.PayLens.user.domain.UserRole;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {
    private Long id;
    private String username;
    private String providerId;
    private UserRole role;
    private String accessToken;

    public static AuthResponse from(User user, String accessToken) {
        return AuthResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .providerId(user.getProviderId())
                .role(user.getRole())
                .accessToken(accessToken)
                .build();
    }
}
