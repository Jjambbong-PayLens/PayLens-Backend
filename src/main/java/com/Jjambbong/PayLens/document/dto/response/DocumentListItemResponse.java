package com.Jjambbong.PayLens.document.dto.response;

import com.Jjambbong.PayLens.document.domain.Document;
import com.Jjambbong.PayLens.document.domain.DocumentStatus;
import com.Jjambbong.PayLens.document.domain.DocumentType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DocumentListItemResponse {
    private Long documentId;
    private String fileName;
    private String contentType;
    private DocumentType documentType;
    private DocumentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DocumentListItemResponse from(Document document) {
        return DocumentListItemResponse.builder()
                .documentId(document.getId())
                .fileName(document.getOriginalFileName())
                .contentType(document.getContentType())
                .documentType(document.getDocumentType())
                .status(document.getStatus())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
}
