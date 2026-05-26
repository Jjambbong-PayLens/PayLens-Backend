package com.Jjambbong.PayLens.document.dto.response;

import java.util.List;

public record OcrComparisonResult(
        Long documentId,
        List<OcrResult> results,
        String recommendedEngine
) {
}
