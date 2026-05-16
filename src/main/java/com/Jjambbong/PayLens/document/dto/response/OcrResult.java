package com.Jjambbong.PayLens.document.dto.response;

import java.util.List;

public record OcrResult(
        String text,
        Double confidence,
        List<OcrLine> lines
) {
}
