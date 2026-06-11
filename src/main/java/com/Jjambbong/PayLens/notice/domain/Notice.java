package com.Jjambbong.PayLens.notice.domain;

import com.Jjambbong.PayLens.Entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notices")
public class Notice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notice_id")
    private Long id;

    @Column(nullable = false)
    private String title;

    @Lob // 내용을 길게 쓸 수 있도록 @Lob 추가
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = true) // 썸네일은 선택사항
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NoticeCategory category;

    @Builder
    public Notice(String title, String content, String thumbnailUrl, NoticeCategory category) {
        this.title = title;
        this.content = content;
        this.thumbnailUrl = thumbnailUrl;
        this.category = category;
    }

    public void update(String title, String content, String thumbnailUrl, NoticeCategory category) {
        this.title = title;
        this.content = content;
        this.thumbnailUrl = thumbnailUrl;
        this.category = category;
    }
}