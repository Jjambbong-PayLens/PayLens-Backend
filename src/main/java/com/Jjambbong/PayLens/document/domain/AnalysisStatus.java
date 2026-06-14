package com.Jjambbong.PayLens.document.domain;

public enum AnalysisStatus {
    READY_FOR_ANALYSIS, // 필드 추출 및 교차 검증을 통과하여 분석 가능
    USER_REVIEW_REQUIRED, // 사용자 검증 또는 재촬영 안내 필요
    REVIEW_COMPLETED, // 사용자가 추출 필드를 확인하거나 수정한 뒤 최종 분석 대기
    COMPLETED, // 최종 Gemini 분석까지 완료되어 결과 조회가 가능
    FAILED // 필드 추출, 사용자 검증, 최종 분석 중 오류가 발생
}
