package com.Jjambbong.PayLens.login.domain;

import jakarta.persistence.*;
import com.Jjambbong.PayLens.user.domain.User;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id", nullable = false)
    private Long tokenId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String refreshToken;

    @Column(nullable = false)
    private Long refreshTokenExpiration;

    @Builder
    public RefreshToken(User user, String refreshToken, Long refreshTokenExpiration) {
        this.user = user;
        this.refreshToken = refreshToken;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public void updateToken(String refreshToken, Long refreshTokenExpiration) {
        this.refreshToken = refreshToken;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }
}
