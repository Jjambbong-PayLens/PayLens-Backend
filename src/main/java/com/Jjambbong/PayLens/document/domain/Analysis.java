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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "analyses")
public class Analysis extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analysis_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AnalysisStatus status;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String fieldExtractionJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String reviewedFieldsJson;

    @Lob
    @Column(name = "result_json", columnDefinition = "TEXT")
    private String resultJson;

    @Column(length = 500)
    private String failureReason;

    @OneToMany(mappedBy = "analysis")
    private List<Document> documents = new ArrayList<>();

    @Builder
    public Analysis(User user, String fieldExtractionJson, String reviewedFieldsJson,
                    String resultJson, AnalysisStatus status, String failureReason) {
        this.user = user;
        this.fieldExtractionJson = fieldExtractionJson;
        this.reviewedFieldsJson = reviewedFieldsJson;
        this.resultJson = resultJson != null ? resultJson : "{}";
        this.status = status != null ? status : AnalysisStatus.COMPLETED;
        this.failureReason = failureReason;
    }

    public void addDocument(Document document) {
        this.documents.add(document);
        if (document.getAnalysis() != this) {
            document.setAnalysis(this);
        }
    }

    public void completeWithResult(String resultJson) {
        this.resultJson = resultJson;
        this.status = AnalysisStatus.COMPLETED;
        this.failureReason = null;
    }

    public void completeReview(String reviewedFieldsJson) {
        this.reviewedFieldsJson = reviewedFieldsJson;
        this.status = AnalysisStatus.REVIEW_COMPLETED;
        this.failureReason = null;
    }

    public void fail(String failureReason) {
        this.status = AnalysisStatus.FAILED;
        this.failureReason = failureReason;
    }
}
