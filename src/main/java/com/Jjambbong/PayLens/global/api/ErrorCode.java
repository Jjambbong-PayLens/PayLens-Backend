package com.Jjambbong.PayLens.global.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode implements BaseCode { // 실패
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_400", "잘못된 요청입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "서버 에러"),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_4041", "존재하지 않는 회원입니다."),
    USER_NOT_FOUND_BY_EMAIL(HttpStatus.NOT_FOUND, "USER_4042", "EMAIL이 존재하지 않는 회원입니다."),
    USER_NOT_FOUND_BY_USERNAME(HttpStatus.NOT_FOUND, "USER_4043", "USERNAME이 존재하지 않는 회원입니다."),

    // Login
    WRONG_REFRESH_TOKEN(HttpStatus.NOT_FOUND, "JWT_4041", "일치하는 refresh token이 없습니다."),
    IP_NOT_MATCHED(HttpStatus.FORBIDDEN, "JWT_4031", "refresh token의 IP주소가 일치하지 않습니다."),
    TOKEN_INVALID(HttpStatus.FORBIDDEN, "JWT_4032", "유효하지 않은 token입니다."),
    TOKEN_NO_AUTH(HttpStatus.FORBIDDEN, "JWT_4033", "권한 정보가 없는 token입니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "JWT_4011", "token 유효기간이 만료되었습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "AUTH_4091", "해당 이메일로 이미 가입된 계정이 존재합니다. 다른 로그인 방식을 시도해 주세요."), // 추가

    // Kakao Login
    KAKAO_AUTH_FAILED(HttpStatus.UNAUTHORIZED, "KAKAO_4011", "카카오 인증에 실패했습니다."),
    KAKAO_API_FAILED(HttpStatus.BAD_GATEWAY, "KAKAO_5021", "카카오 서버 응답에 실패했습니다."),

    // Google Login
    GOOGLE_AUTH_FAILED(HttpStatus.UNAUTHORIZED, "GOOGLE_4011", "구글 인증에 실패했습니다."),
    GOOGLE_API_FAILED(HttpStatus.BAD_GATEWAY, "GOOGLE_5021", "구글 서버 응답에 실패했습니다."),

    // Notice
    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTICE_4041", "공지사항을 찾을 수 없습니다."),

    // Document
    DOCUMENT_UPLOAD_FILE_COUNT_INVALID(HttpStatus.BAD_REQUEST, "DOCUMENT_4003", "문서 업로드 파일 개수는 1개 이상 10개 이하이어야 합니다."),
    DOCUMENT_COMPLETE_COUNT_INVALID(HttpStatus.BAD_REQUEST, "DOCUMENT_4004", "완료 처리할 문서 개수는 1개 이상 10개 이하이어야 합니다."),
    DOCUMENT_DELETE_COUNT_INVALID(HttpStatus.BAD_REQUEST, "DOCUMENT_4005", "삭제할 문서 개수는 1개 이상 10개 이하이어야 합니다."),
    DOCUMENT_ANALYZE_COUNT_INVALID(HttpStatus.BAD_REQUEST, "DOCUMENT_4006", "분석할 문서 개수는 1개 이상 10개 이하이어야 합니다."),
    DOCUMENT_INVALID_FILE_NAME(HttpStatus.BAD_REQUEST, "DOCUMENT_4001", "올바르지 않은 파일명입니다."),
    DOCUMENT_CONTENT_TYPE_NOT_ALLOWED(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "DOCUMENT_4151", "허용되지 않은 파일 형식입니다."),
    DOCUMENT_TYPE_INVALID(HttpStatus.BAD_REQUEST, "DOCUMENT_4002", "올바르지 않은 문서 유형입니다."),
    DOCUMENT_UPLOAD_NOT_READY(HttpStatus.CONFLICT, "DOCUMENT_4091", "업로드 완료 처리할 수 없는 문서 상태입니다."),
    DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "DOCUMENT_4041", "문서를 찾을 수 없습니다."),
    DOCUMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "DOCUMENT_4031", "해당 문서에 접근할 권한이 없습니다."),
    DOCUMENT_OBJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "DOCUMENT_4042", "S3에서 업로드된 파일을 찾을 수 없습니다."),
    DOCUMENT_OCR_COUNT_INVALID(HttpStatus.BAD_REQUEST, "DOCUMENT_4007", "OCR 처리할 문서 개수는 1개 이상 10개 이하이어야 합니다."),
    DOCUMENT_OCR_TARGET_INVALID(HttpStatus.CONFLICT, "DOCUMENT_4092", "OCR 처리할 수 없는 문서 상태입니다."),
    DOCUMENT_OCR_ALREADY_PROCESSING(HttpStatus.CONFLICT, "DOCUMENT_4093", "이미 OCR 처리가 진행 중인 문서입니다."),
    PRESIGNED_URL_GENERATION_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "DOCUMENT_5031", "문서 업로드 URL 발급에 실패했습니다."),
    OCR_REQUEST_FAILED(HttpStatus.BAD_GATEWAY, "OCR_5021", "OCR 서버 요청에 실패했습니다."),
    OCR_RESPONSE_INVALID(HttpStatus.BAD_GATEWAY, "OCR_5022", "OCR 서버 응답이 올바르지 않습니다."),
    OCR_RESULT_KEY_MISMATCH(HttpStatus.BAD_GATEWAY, "OCR_5023", "OCR 결과 저장 경로가 일치하지 않습니다."),
    ANALYSIS_NOT_FOUND(HttpStatus.NOT_FOUND, "DOCUMENT_4043", "아직 해당 문서에 대한 분석 결과가 없습니다."),
    DOCUMENT_ANALYZE_OCR_REQUIRED(HttpStatus.CONFLICT, "DOCUMENT_4094", "OCR 완료 후 분석할 수 있습니다."),
    GEMINI_FIELD_EXTRACTION_FAILED(HttpStatus.BAD_GATEWAY, "GEMINI_5021", "Gemini 필드 추출 요청에 실패했습니다."),
    GEMINI_RESPONSE_INVALID(HttpStatus.BAD_GATEWAY, "GEMINI_5022", "Gemini 응답이 올바르지 않습니다."),
    GEMINI_REVIEW_STATUS_INVALID(HttpStatus.CONFLICT, "GEMINI_4091", "사용자 검증을 처리할 수 없는 분석 상태입니다."),
    GEMINI_ANALYZE_STATUS_INVALID(HttpStatus.CONFLICT, "GEMINI_4092", "최종 분석을 실행할 수 없는 분석 상태입니다."),

    // YouTube
    YOUTUBE_API_FAILED(HttpStatus.BAD_GATEWAY, "YOUTUBE_API_FAILED", "YouTube API 호출에 실패했습니다."),
    SONG_ALREADY_SAVED(HttpStatus.CONFLICT, "SONG_ALREADY_SAVED", "이미 저장된 곡입니다."),
    SONG_NOT_FOUND(HttpStatus.NOT_FOUND, "TODO_4041", "해당 노래를 찾을 수 없습니다."),

    // Image Upload
    IMAGE_FILE_EMPTY(HttpStatus.BAD_REQUEST, "IMG_4001", "업로드된 파일이 비어 있습니다."),
    IMAGE_TYPE_NOT_ALLOWED(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "IMG_4151", "허용되지 않은 이미지 형식입니다."),
    IMAGE_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "IMG_4131", "이미지 파일 용량이 너무 큽니다."),
    IMAGE_PROCESS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "IMG_5001", "이미지 처리에 실패했습니다."),

    // S3 Upload
    S3_UPLOAD_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "S3_5031", "파일 저장에 실패했습니다."),
    S3_KEY_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S3_5001", "파일 키 생성에 실패했습니다."),
    S3_DELETE_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "S3_5032", "파일 삭제에 실패했습니다."),

    //follow
    FOLLOW_SELF_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "FOLLOW_4001", "자기 자신을 팔로우할 수 없습니다."),
    FOLLOW_TARGET_NOT_FOUND(HttpStatus.NOT_FOUND, "FOLLOW_4041", "팔로우 대상 사용자가 존재하지 않습니다."),
    FOLLOW_ALREADY_EXISTS(HttpStatus.CONFLICT, "FOLLOW_4091", "이미 팔로우한 사용자입니다."),
    FOLLOW_NOT_FOUND(HttpStatus.NOT_FOUND, "FOLLOW_4042", "팔로우 관계가 존재하지 않습니다."),
    INVALID_HANDLE_FORMAT(HttpStatus.BAD_REQUEST, "F002", "유저 검색 형식이 올바르지 않습니다. (예: 김동현#1234)"),

    // Category
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "CATEGORY_4041", "해당 카테고리를 찾을 수 없습니다."),

    // 투두
    TODO_NOT_FOUND(HttpStatus.NOT_FOUND, "TODO_4041", "해당 투두를 찾을 수 없습니다."),
    TODO_ACCESS_DENIED(HttpStatus.FORBIDDEN, "TODO_4031", "해당 투두에 접근할 권한이 없습니다."),

    TODO_ROUTINE_END_DATE_REQUIRED(HttpStatus.BAD_REQUEST, "TODO_4001", "종료 날짜 입력은 필수 입니다."),
    TODO_ROUTINE_DATE_RANGE_INVALID(HttpStatus.BAD_REQUEST, "TODO_4002", "시작 날짜는 종료 날짜의 이전이어야 합니다."),
    TODO_ROUTINE_WEEK_REQUIRED(HttpStatus.BAD_REQUEST, "TODO_4003", "요일 선택은 필수 입니다."),
    TODO_ROUTINE_DAY_OF_WEEK_INVALID(HttpStatus.BAD_REQUEST, "TODO_4004", "옳지 않은 요일입니다."),

    TODO_DESCRIPTION_REQUIRED(HttpStatus.BAD_REQUEST, "TODO_4005", "투두 내용 입력은 필수 입니다."),
    TODO_CATEGORY_REQUIRED(HttpStatus.BAD_REQUEST, "TODO_4006", "카테고리 선택은 필수 입니다."),

    TODO_ROUTINE_RULE_INCONSISTENT(HttpStatus.CONFLICT, "TODO_4091", "루틴 규칙 데이터가 올바르지 않습니다."),
    TODO_DATE_REQUIRED(HttpStatus.BAD_REQUEST, "TODO_4007", "일반 투두는 날짜 입력이 필수입니다."),
    TODO_ROUTINE_TO_NORMAL_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "TODO_4008", "루틴 투두는 일반 투두로 변경할 수 없습니다. 필요하면 루틴 투두를 삭제하고 원하는 날짜로 일반 투두를 새로 생성해주세요."),
    TODO_DATE_NOT_FOUND(HttpStatus.NOT_FOUND, "TODO_4042", "해당 날짜의 투두 기록을 찾을 수 없습니다."),

    // Labor (노무사 관련)
    PENDING_ALREADY_EXISTS(HttpStatus.CONFLICT, "LABOR_4091", "이미 노무사 승인 대기 중인 상태입니다."),
    ALREADY_APPROVED_LABOR(HttpStatus.CONFLICT, "LABOR_4092", "이미 승인된 노무사 회원입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    // 응답 코드 상세 정보 return
    @Override
    public ReasonDTO getReason() {
        return ReasonDTO.builder()
                .httpStatus(this.httpStatus)
                .code(this.code)
                .message(this.message)
                .build();
    }
}
