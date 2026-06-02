package com.Jjambbong.PayLens.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.Jjambbong.PayLens.document.domain.DocumentType;
import com.Jjambbong.PayLens.document.dto.request.DocumentUploadUrlRequest;
import com.Jjambbong.PayLens.global.api.ErrorCode;
import com.Jjambbong.PayLens.global.exception.GeneralException;
import org.junit.jupiter.api.Test;

class DocumentUploadPolicyTest {

    private final DocumentUploadPolicy policy = new DocumentUploadPolicy();

    @Test
    void acceptsAllowedFileAndSanitizesFileName() {
        DocumentUploadUrlRequest request = new DocumentUploadUrlRequest(
                "../pay stub.pdf",
                "Application/PDF",
                "payslip"
        );

        DocumentUploadFile result = policy.validate(request);

        assertThat(result.originalFileName()).isEqualTo("../pay stub.pdf");
        assertThat(result.sanitizedFileName()).isEqualTo("pay_stub.pdf");
        assertThat(result.contentType()).isEqualTo("application/pdf");
        assertThat(result.documentType()).isEqualTo(DocumentType.PAYSLIP);
    }

    @Test
    void rejectsUnsupportedContentType() {
        DocumentUploadUrlRequest request = new DocumentUploadUrlRequest(
                "paystub.exe",
                "application/x-msdownload",
                "PAYSLIP"
        );

        assertThatThrownBy(() -> policy.validate(request))
                .isInstanceOfSatisfying(GeneralException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(ErrorCode.DOCUMENT_CONTENT_TYPE_NOT_ALLOWED));
    }

    @Test
    void rejectsInvalidDocumentType() {
        DocumentUploadUrlRequest request = new DocumentUploadUrlRequest(
                "paystub.pdf",
                "application/pdf",
                "UNKNOWN"
        );

        assertThatThrownBy(() -> policy.validate(request))
                .isInstanceOfSatisfying(GeneralException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(ErrorCode.DOCUMENT_TYPE_INVALID));
    }
}
