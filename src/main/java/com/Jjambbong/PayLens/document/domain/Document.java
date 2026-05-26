package com.Jjambbong.PayLens.document.domain;

import com.Jjambbong.PayLens.Entity.BaseEntity;
import com.Jjambbong.PayLens.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "documents")
public class Document extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id")
    private Analysis analysis;

    @Column(nullable = false, length = 255)
    private String originalFileName;

    @Column(nullable = false, length = 300)
    private String storedFileName;

    @Column(nullable = false, unique = true, length = 700)
    private String objectKey;

    @Column(nullable = false, length = 150)
    private String contentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DocumentType documentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DocumentStatus status;

    @Builder
    public Document(User user, String originalFileName, String storedFileName, String objectKey,
                    String contentType, DocumentType documentType, DocumentStatus status) {
        this.user = user;
        this.originalFileName = originalFileName;
        this.storedFileName = storedFileName;
        this.objectKey = objectKey;
        this.contentType = contentType;
        this.documentType = documentType;
        this.status = status;
    }

    public void completeUpload() {
        this.status = DocumentStatus.UPLOADED;
    }

    public void setAnalysis(Analysis analysis) {
        this.analysis = analysis;
    }
}
