package com.Jjambbong.PayLens.document.client;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OcrAnalyzeFastApiResult {
    private Long documentId;
    private String fileName;
    private String contentType;
    private String model;
    private String ocrResultKey;
    private Boolean uploaded;
    private Long ocrJsonSizeBytes;
    private String processedAt;
}
