package com.Jjambbong.PayLens.document.client;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OcrAnalyzeFastApiResponse {
    private Boolean isSuccess;
    private String code;
    private String message;
    private OcrAnalyzeFastApiResult result;

    public boolean isSuccessfulUploadResponse() {
        return Boolean.TRUE.equals(isSuccess) && "OCR_2003".equals(code) && result != null;
    }
}
