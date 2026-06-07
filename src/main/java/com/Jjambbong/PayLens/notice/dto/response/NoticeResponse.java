package com.Jjambbong.PayLens.notice.dto.response;

import com.Jjambbong.PayLens.notice.domain.Notice;
import com.Jjambbong.PayLens.notice.domain.NoticeCategory;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class NoticeResponse {

    private final Long noticeId;
    private final String title;
    private final String content;
    private final String thumbnailUrl;
    private final NoticeCategory category;
    private final LocalDateTime createdAt;

    public NoticeResponse(Notice notice) {
        this.noticeId = notice.getId();
        this.title = notice.getTitle();
        this.content = notice.getContent();
        this.thumbnailUrl = notice.getThumbnailUrl();
        this.category = notice.getCategory();
        this.createdAt = notice.getCreatedAt();
    }
}
