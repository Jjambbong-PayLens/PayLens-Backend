package com.Jjambbong.PayLens.document.dto.request;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DocumentAnalyzeRequest {
    private List<Long> documentIds;
}
