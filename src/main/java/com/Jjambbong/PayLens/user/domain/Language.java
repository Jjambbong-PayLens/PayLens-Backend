package com.Jjambbong.PayLens.user.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Language {
    KO("ko", "한국어"),
    EN("en", "English"),
    VI("vi", "Tiếng Việt");

    private final String code;
    private final String description;
}
