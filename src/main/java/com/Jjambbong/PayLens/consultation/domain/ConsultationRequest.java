package com.Jjambbong.PayLens.consultation.domain;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "consultation_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConsultationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId; // 유저 ID 값만 들고 있도록 설계합니다.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultant_id", nullable = false)
    private LaborConsultant laborConsultant;

    @Column(name = "report_file_url", length = 1024)
    private String reportFileUrl;

    private String status = "PENDING";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    @Builder
    public ConsultationRequest(Long userId, LaborConsultant laborConsultant, String reportFileUrl) {
        this.userId = userId;
        this.laborConsultant = laborConsultant;
        this.reportFileUrl = reportFileUrl;
        this.status = "PENDING";
    }

    // 메일 발송 성공 시 상태를 바꿀 때 사용할 메서드
    public void completeRequest() {
        this.status = "COMPLETED";
    }

    // 메일 발송 실패 시 상태를 바꿀 때 사용할 메서드
    public void failRequest() {
        this.status = "FAILED";
    }
}