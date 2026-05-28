package com.Jjambbong.PayLens.document.client;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OcrAnalyzeFastApiRequest {
    private Long userId;
    private Long documentId;
    private String fileName;
    private String contentType;
    private String documentType;
    private String downloadUrl;
    private String uploadUrl;
    private String ocrResultKey;
}
