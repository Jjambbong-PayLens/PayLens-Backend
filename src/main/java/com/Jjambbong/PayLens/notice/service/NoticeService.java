package com.Jjambbong.PayLens.notice.service;

import com.Jjambbong.PayLens.global.api.ErrorCode;
import com.Jjambbong.PayLens.global.exception.GeneralException;
import com.Jjambbong.PayLens.notice.domain.Notice;
import com.Jjambbong.PayLens.notice.dto.request.NoticeRequest;
import com.Jjambbong.PayLens.notice.dto.response.NoticeListItemResponse;
import com.Jjambbong.PayLens.notice.dto.response.NoticeResponse;
import com.Jjambbong.PayLens.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class NoticeService {

    private final NoticeRepository noticeRepository;

    @Transactional(readOnly = true)
    public List<NoticeListItemResponse> getNotices() {
        return noticeRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(NoticeListItemResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NoticeResponse getNotice(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new GeneralException(ErrorCode.NOTICE_NOT_FOUND));
        return new NoticeResponse(notice);
    }

    // 공지사항 생성
    public Notice createNotice(NoticeRequest request) {
        Notice notice = Notice.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .thumbnailUrl(request.getThumbnailUrl())
                .category(request.getCategory())
                .build();
        return noticeRepository.save(notice);
    }

    // 공지사항 수정
    public Notice updateNotice(Long noticeId, NoticeRequest request) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new GeneralException(ErrorCode.NOTICE_NOT_FOUND)); // 에러코드 추가 필요

        notice.update(
                request.getTitle(),
                request.getContent(),
                request.getThumbnailUrl(),
                request.getCategory()
        );
        return notice;
    }

    // 공지사항 삭제
    public void deleteNotice(Long noticeId) {
        if (!noticeRepository.existsById(noticeId)) {
            throw new GeneralException(ErrorCode.NOTICE_NOT_FOUND);
        }
        noticeRepository.deleteById(noticeId);
    }
}