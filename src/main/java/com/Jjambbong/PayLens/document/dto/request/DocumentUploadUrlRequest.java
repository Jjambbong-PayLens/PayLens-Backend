package com.Jjambbong.PayLens.document.dto.request;

import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DocumentUploadUrlRequest {
    private String fileName;
    private String contentType;
    private String documentType;
}
