package com.Jjambbong.PayLens.user.repository;

import com.Jjambbong.PayLens.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {


    Optional<User> findByProviderId(String providerId);


    Optional<User> findByEmail(String email);
}
