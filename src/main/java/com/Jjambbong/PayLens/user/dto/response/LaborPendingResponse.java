package com.Jjambbong.PayLens.user.dto.response;

import com.Jjambbong.PayLens.user.domain.LaborApproveStatus;
import com.Jjambbong.PayLens.user.domain.Language;
import com.Jjambbong.PayLens.user.domain.User;
import com.Jjambbong.PayLens.user.domain.UserRole;
import com.Jjambbong.PayLens.user.domain.UserStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LaborPendingResponse {

    private final Long userId;
    private final String providerId;
    private final String email;
    private final String username;
    private final UserRole role;
    private final UserStatus status;
    private final Language preferredLanguage;
    private final LaborApproveStatus laborApproveStatus;
    private final LocalDateTime createdAt;

    public static LaborPendingResponse from(User user) {
        return LaborPendingResponse.builder()
                .userId(user.getId())
                .providerId(user.getProviderId())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole())
                .status(user.getStatus())
                .preferredLanguage(user.getPreferredLanguage())
                .laborApproveStatus(user.getLaborApproveStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
