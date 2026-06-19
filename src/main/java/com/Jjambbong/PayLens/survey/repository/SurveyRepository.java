package com.Jjambbong.PayLens.survey.repository;

import com.Jjambbong.PayLens.survey.domain.Survey;
import com.Jjambbong.PayLens.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SurveyRepository extends JpaRepository<Survey, Long> {
    Optional<Survey> findByUser(User user);
}
