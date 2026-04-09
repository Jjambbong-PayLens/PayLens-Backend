package com.Jjambbong.PayLens.user.domain;

import jakarta.persistence.*;
import lombok.*;
import com.Jjambbong.PayLens.Entity.BaseEntity;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long Id;

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

    @Column(name = "preferred_language", length = 10)
    private String preferredLanguage;

    @Builder
    public User(String providerId, String email, String username, UserRole role,
                UserStatus status, String preferredLanguage) {
        this.providerId = providerId;
        this.email = email;
        this.username = username;
        this.role = role;
        this.status = status;
        this.preferredLanguage = preferredLanguage;
    }

    public void updateRole(UserRole role) {
        this.role = role;
    }

    public void updateStatus(UserStatus status) {
        this.status = status;
    }

    public void updatePreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }
}