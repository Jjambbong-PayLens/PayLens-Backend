package com.Jjambbong.PayLens.document.repository;

import com.Jjambbong.PayLens.document.domain.Document;
import com.Jjambbong.PayLens.document.domain.DocumentStatus;
import com.Jjambbong.PayLens.user.domain.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByUserAndStatusOrderByCreatedAtDesc(User user, DocumentStatus status);
}
