package com.Jjambbong.PayLens.document.dto.response;

public record OcrLine(
        String text,
        Double confidence
) {
}
