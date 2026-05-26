package com.Jjambbong.PayLens.document.domain;

import com.Jjambbong.PayLens.Entity.BaseEntity;
import com.Jjambbong.PayLens.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

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

    @Lob
    @Column(name = "result_json", columnDefinition = "TEXT", nullable = false)
    private String resultJson;

    @OneToMany(mappedBy = "analysis", cascade = CascadeType.ALL)
    private List<Document> documents = new ArrayList<>();

    @Builder
    public Analysis(User user, String resultJson) {
        this.user = user;
        this.resultJson = resultJson;
    }

    // 연관관계 편의 메서드
    public void addDocument(Document document) {
        this.documents.add(document);
        if (document.getAnalysis() != this) {
            document.setAnalysis(this);
        }
    }
}
