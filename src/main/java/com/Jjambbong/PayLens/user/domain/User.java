package com.Jjambbong.PayLens.user.domain;

import com.Jjambbong.PayLens.document.domain.Document;
import com.Jjambbong.PayLens.payment.domain.Payment;
import com.Jjambbong.PayLens.survey.domain.Survey;
import jakarta.persistence.*;
import lombok.*;
import com.Jjambbong.PayLens.Entity.BaseEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long Id;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Survey survey;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Payment> payments = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Document> documents = new ArrayList<>();

    @Column(nullable = false, unique = true)
    private String providerId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_language", length = 10)
    private Language preferredLanguage;

    // 노무사 승인 상태 필드
    @Enumerated(EnumType.STRING)
    @Column(name = "labor_approve_status", length = 20, nullable = false)
    private LaborApproveStatus laborApproveStatus;

    @Builder
    public User(String providerId, String email, String username, UserRole role,
                UserStatus status, Language preferredLanguage,
                LaborApproveStatus laborApproveStatus) { // Builder 파라미터
        this.providerId = providerId;
        this.email = email;
        this.username = username;
        this.role = role;
        this.status = status;
        this.preferredLanguage = preferredLanguage;
        // 값이 없을 경우 기본값 NONE으로 세팅
        this.laborApproveStatus = laborApproveStatus != null ? laborApproveStatus : LaborApproveStatus.NONE;
    }

    public void updateRole(UserRole role) {
        this.role = role;
    }

    public void updateStatus(UserStatus status) {
        this.status = status;
    }

    public void updatePreferredLanguage(Language preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    //노무사 승인 상태를 변경할 수 있는 메서드
    public void updateLaborApproveStatus(LaborApproveStatus status) {
        this.laborApproveStatus = status;
    }
}