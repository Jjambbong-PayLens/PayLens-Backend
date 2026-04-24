package com.Jjambbong.PayLens.document.repository;

import com.Jjambbong.PayLens.document.domain.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {
}
