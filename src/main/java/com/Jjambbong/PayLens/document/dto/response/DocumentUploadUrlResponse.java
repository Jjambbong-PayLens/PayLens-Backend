package com.Jjambbong.PayLens.document.dto.response;

import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DocumentUploadUrlResponse {
    private Long documentId;
    private String uploadUrl;
    private String objectKey;
    private Long expiresInSeconds;
    private Map<String, String> requiredHeaders;
}
