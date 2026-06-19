package com.Jjambbong.PayLens.document.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DocumentOcrListResponse {
    private List<DocumentOcrResponse> documents;
    private int count;
}
