package com.Jjambbong.PayLens.user.dto.response;

import com.Jjambbong.PayLens.user.domain.User;
import com.Jjambbong.PayLens.user.domain.UserRole;
import lombok.Getter;

@Getter
public class UserInfoResponse {
    private final Long userId;
    private final String username;
    private final String email;
    private final UserRole role;

    public UserInfoResponse(User user) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.role = user.getRole();
    }
}
