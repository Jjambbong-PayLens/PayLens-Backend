package com.Jjambbong.PayLens.document.dto.response;

import com.Jjambbong.PayLens.document.service.ocr.OcrEngineType;

import java.util.List;

public record OcrResult(
        OcrEngineType engineType,
        String text,
        Double confidence,
        Long elapsedMs,
        List<OcrLine> lines
) {
}
