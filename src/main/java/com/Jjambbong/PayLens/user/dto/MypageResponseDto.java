package com.Jjambbong.PayLens.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MypageResponseDto {
    private String username;
    private String email;
    private String preferredLanguage;

    public MypageResponseDto(String username, String email, String preferredLanguage) {
        this.username = username;
        this.email = email;
        this.preferredLanguage = preferredLanguage;
    }
}