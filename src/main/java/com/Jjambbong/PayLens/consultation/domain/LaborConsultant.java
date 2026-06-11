package com.Jjambbong.PayLens.consultation.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "labor_consultants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class LaborConsultant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "consultant_id")
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "office_name", nullable = false)
    private String officeName;

    private String phone;

    @Column(nullable = false)
    private String email;

    @Column(name = "kakao_channel")
    private String kakaoChannel;

    private String region;

    private String status = "ACTIVE";

    @Lob
    private String specialties;

    @Column(name = "supported_languages", columnDefinition = "json")
    private String supportedLanguages;

    @Lob
    private String introduction;

    @CreationTimestamp // 데이터가 생성될 때 현재 시간이 자동으로 들어간다
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp // 데이터가 수정될 때 시간이 자동으로 업데이트된다
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void update(String name, String officeName, String phone, String email,
                       String kakaoChannel, String region, String specialties,
                       String supportedLanguages, String introduction, String status) {
        this.name = name;
        this.officeName = officeName;
        this.phone = phone;
        this.email = email;
        this.kakaoChannel = kakaoChannel;
        this.region = region;
        this.specialties = specialties;
        this.supportedLanguages = supportedLanguages;
        this.introduction = introduction;
        this.status = status;
    }
}