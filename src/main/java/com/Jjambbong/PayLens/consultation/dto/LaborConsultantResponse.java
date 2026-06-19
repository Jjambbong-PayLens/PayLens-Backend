package com.Jjambbong.PayLens.consultation.dto;

import com.Jjambbong.PayLens.consultation.domain.LaborConsultant;
import lombok.Getter;

@Getter
public class LaborConsultantResponse {

    private final Long id;
    private final String name;
    private final String officeName;
    private final String phone;
    private final String email;
    private final String kakaoChannel;
    private final String region;
    private final String specialties;
    private final String supportedLanguages;
    private final String introduction;

    // Entity(DB 데이터)를 DTO(응답 데이터)로 변환해 주는 생성자
    public LaborConsultantResponse(LaborConsultant consultant) {
        this.id = consultant.getId();
        this.name = consultant.getName();
        this.officeName = consultant.getOfficeName();
        this.phone = consultant.getPhone();
        this.email = consultant.getEmail();
        this.kakaoChannel = consultant.getKakaoChannel();
        this.region = consultant.getRegion();
        this.specialties = consultant.getSpecialties();
        this.supportedLanguages = consultant.getSupportedLanguages();
        this.introduction = consultant.getIntroduction();
    }
}