package com.Jjambbong.PayLens.login.repository;

import com.Jjambbong.PayLens.login.domain.RefreshToken;
import com.Jjambbong.PayLens.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken,Long> {
    Optional<RefreshToken> findByUser(User user);

    void deleteByUser(User user);
}