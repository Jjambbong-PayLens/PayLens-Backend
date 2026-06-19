package com.Jjambbong.PayLens.consultation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LaborConsultantRequest {

    @NotBlank(message = "노무사 이름은 필수 입력값입니다.")
    private String name;

    @NotBlank(message = "노무법인/사무소명은 필수 입력값입니다.")
    private String officeName;

    private String phone;

    @NotBlank(message = "이메일은 필수 입력값입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    private String kakaoChannel;
    private String region;
    private String specialties;
    private String supportedLanguages;
    private String introduction;
}