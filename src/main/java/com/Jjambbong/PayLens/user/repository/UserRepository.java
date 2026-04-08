package com.Jjambbong.PayLens.user.repository;

import com.Jjambbong.PayLens.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByProviderId(String providerId);
    Optional<User> findById(Long id);
}
