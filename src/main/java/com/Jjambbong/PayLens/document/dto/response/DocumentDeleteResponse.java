package com.Jjambbong.PayLens.document.dto.response;

import com.Jjambbong.PayLens.document.domain.Document;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DocumentDeleteResponse {
    private Long documentId;
    private String fileName;

    public static DocumentDeleteResponse from(Document document) {
        return DocumentDeleteResponse.builder()
                .documentId(document.getId())
                .fileName(document.getOriginalFileName())
                .build();
    }
}
