package com.Jjambbong.PayLens.document.service;

import com.Jjambbong.PayLens.document.domain.DocumentType;

public record DocumentUploadFile(
        String originalFileName,
        String sanitizedFileName,
        String contentType,
        DocumentType documentType
) {
}
