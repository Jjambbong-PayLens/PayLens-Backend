package com.Jjambbong.PayLens.document.dto.response;

import com.Jjambbong.PayLens.document.domain.Document;
import com.Jjambbong.PayLens.document.domain.DocumentStatus;
import com.Jjambbong.PayLens.document.domain.DocumentType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DocumentCompleteResponse {
    private Long documentId;
    private DocumentStatus status;
    private DocumentType documentType;
    private String fileName;

    public static DocumentCompleteResponse from(Document document) {
        return DocumentCompleteResponse.builder()
                .documentId(document.getId())
                .status(document.getStatus())
                .documentType(document.getDocumentType())
                .fileName(document.getOriginalFileName())
                .build();
    }
}
