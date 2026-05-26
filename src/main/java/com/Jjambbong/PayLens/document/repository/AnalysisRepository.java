package com.Jjambbong.PayLens.document.repository;

import com.Jjambbong.PayLens.document.domain.Analysis;
import com.Jjambbong.PayLens.document.domain.Document;
import com.Jjambbong.PayLens.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnalysisRepository extends JpaRepository<Analysis, Long> {
    Optional<Analysis> findByDocumentsContains(Document document);
    List<Analysis> findByUserOrderByCreatedAtDesc(User user);
}
