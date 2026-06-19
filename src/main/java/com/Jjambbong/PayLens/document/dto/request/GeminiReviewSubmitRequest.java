package com.Jjambbong.PayLens.document.dto.request;

import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GeminiReviewSubmitRequest {

    private List<DocumentGroupReviewRequest> documentGroups;

    public GeminiReviewSubmitRequest(List<DocumentGroupReviewRequest> documentGroups) {
        this.documentGroups = documentGroups;
    }

    @Getter
    @NoArgsConstructor
    public static class DocumentGroupReviewRequest {

        private String groupId;
        private Map<String, Object> confirmedFields;

        public DocumentGroupReviewRequest(String groupId, Map<String, Object> confirmedFields) {
            this.groupId = groupId;
            this.confirmedFields = confirmedFields;
        }
    }
}
