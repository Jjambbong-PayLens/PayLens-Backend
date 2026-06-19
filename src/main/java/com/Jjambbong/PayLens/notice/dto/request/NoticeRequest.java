package com.Jjambbong.PayLens.notice.dto.request;

import com.Jjambbong.PayLens.notice.domain.NoticeCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NoticeRequest {

    @NotBlank(message = "제목을 입력해주세요.")
    @Schema(description = "공지사항 제목", example = "페이렌즈 서비스 업데이트 안내")
    private String title;

    @NotBlank(message = "내용을 입력해주세요.")
    @Schema(description = "공지사항 내용", example = "안녕하세요, 페이렌즈입니다. 5월 28일 새로운 기능이 추가됩니다.")
    private String content;

    @Schema(description = "썸네일 이미지 URL (선택 사항)", example = "https://paylens.com/images/thumbnail.png")
    private String thumbnailUrl;

    @NotNull(message = "카테고리를 선택해주세요.")
    @Schema(description = "공지사항 카테고리 (NOTICE, EVENT, UPDATE 중 하나)", example = "UPDATE")
    private NoticeCategory category;
}