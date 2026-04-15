package com.Jjambbong.PayLens.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserLanguageUpdateRequest {

    @NotBlank(message = "언어 코드는 비워둘 수 없습니다.")
    @Schema(description = "선호 언어 코드 (예: ko, en, vi)", example = "en")
    private String language;
}