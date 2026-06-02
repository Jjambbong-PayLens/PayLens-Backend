package com.Jjambbong.PayLens.document.service;

import com.Jjambbong.PayLens.document.domain.DocumentType;
import com.Jjambbong.PayLens.document.dto.request.DocumentUploadUrlRequest;
import com.Jjambbong.PayLens.global.api.ErrorCode;
import com.Jjambbong.PayLens.global.exception.GeneralException;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class DocumentUploadPolicy {

    private static final int MAX_FILE_NAME_LENGTH = 150;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/webp",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    public DocumentUploadFile validate(DocumentUploadUrlRequest request) {
        if (request == null) {
            throw new GeneralException(ErrorCode.BAD_REQUEST);
        }

        String contentType = normalizeContentType(request.getContentType());
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new GeneralException(ErrorCode.DOCUMENT_CONTENT_TYPE_NOT_ALLOWED);
        }

        DocumentType documentType = parseDocumentType(request.getDocumentType());
        String originalFileName = validateOriginalFileName(request.getFileName());
        String sanitizedFileName = sanitizeFileName(originalFileName);

        return new DocumentUploadFile(originalFileName, sanitizedFileName, contentType, documentType);
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new GeneralException(ErrorCode.DOCUMENT_CONTENT_TYPE_NOT_ALLOWED);
        }
        return contentType.trim().toLowerCase();
    }

    private DocumentType parseDocumentType(String documentType) {
        if (documentType == null || documentType.isBlank()) {
            throw new GeneralException(ErrorCode.DOCUMENT_TYPE_INVALID);
        }

        try {
            return DocumentType.valueOf(documentType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new GeneralException(ErrorCode.DOCUMENT_TYPE_INVALID);
        }
    }

    private String validateOriginalFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new GeneralException(ErrorCode.DOCUMENT_INVALID_FILE_NAME);
        }
        return fileName.trim();
    }

    private String sanitizeFileName(String fileName) {
        String normalized = fileName.replace("\\", "/");
        int lastSeparator = normalized.lastIndexOf('/');
        if (lastSeparator >= 0) {
            normalized = normalized.substring(lastSeparator + 1);
        }

        String sanitized = normalized
                .trim()
                .replaceAll("\\s+", "_")
                .replaceAll("[^\\p{L}\\p{N}._-]", "_")
                .replaceAll("_+", "_");

        if (sanitized.isBlank() || sanitized.equals(".") || sanitized.equals("..")) {
            throw new GeneralException(ErrorCode.DOCUMENT_INVALID_FILE_NAME);
        }

        if (sanitized.length() > MAX_FILE_NAME_LENGTH) {
            sanitized = sanitized.substring(sanitized.length() - MAX_FILE_NAME_LENGTH);
        }

        return sanitized;
    }
}
