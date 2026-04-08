package com.Jjambbong.PayLens.login.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtProvider {

    private final Key key;
    private final long accessExpMs;
    private final long refreshExpMs;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-exp-ms:3600000}") long accessExpMs,     // 기본 1시간
            @Value("${jwt.refresh-exp-ms:1209600000}") long refreshExpMs // 기본 14일
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpMs = accessExpMs;
        this.refreshExpMs = refreshExpMs;
    }

    public String createAccessToken(Long userId) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + accessExpMs);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String createRefreshToken(Long userId) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + refreshExpMs);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // DB에 저장할 refresh 만료 시각
    public Long getRefreshTokenExpiration() {
        return System.currentTimeMillis() + refreshExpMs;
    }

    // key로 accessToken 복호화
    public void validate(String token) {
        Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
    }

    // key-value에 담은 후 accessToken 복호화 + userId 추출
    public Long getUserId(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return Long.parseLong(claims.getSubject());
    }

    // accessToken 재발급을 위해 만료된 accessToken 전달
    public Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }
}
