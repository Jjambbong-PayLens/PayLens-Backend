package com.Jjambbong.PayLens.document.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DocumentUploadUrlsResponse {
    private List<DocumentUploadUrlResponse> files;
    private int count;
}
