package com.Jjambbong.PayLens.document.dto.response;

import com.Jjambbong.PayLens.document.domain.Document;
import com.Jjambbong.PayLens.document.domain.DocumentOcrStatus;
import com.Jjambbong.PayLens.document.domain.DocumentType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DocumentOcrResponse {
    private Long documentId;
    private String fileName;
    private DocumentType documentType;
    private DocumentOcrStatus ocrStatus;
    private String ocrResultKey;
    private Long ocrJsonSizeBytes;
    private LocalDateTime ocrProcessedAt;

    public static DocumentOcrResponse from(Document document) {
        return DocumentOcrResponse.builder()
                .documentId(document.getId())
                .fileName(document.getOriginalFileName())
                .documentType(document.getDocumentType())
                .ocrStatus(document.getOcrStatus())
                .ocrResultKey(document.getOcrResultKey())
                .ocrJsonSizeBytes(document.getOcrJsonSizeBytes())
                .ocrProcessedAt(document.getOcrProcessedAt())
                .build();
    }
}
