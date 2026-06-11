package com.Jjambbong.PayLens.notice.repository;

import com.Jjambbong.PayLens.notice.domain.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; // 추가

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    // 모든 공지사항을 생성일시 기준 내림차순(최신순)으로 조회
    List<Notice> findAllByOrderByCreatedAtDesc();
}