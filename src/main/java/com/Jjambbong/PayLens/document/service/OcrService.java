package com.Jjambbong.PayLens.document.service;

import com.Jjambbong.PayLens.document.domain.Document;
import com.Jjambbong.PayLens.document.dto.response.OcrComparisonResult;
import com.Jjambbong.PayLens.document.dto.response.OcrLine;
import com.Jjambbong.PayLens.document.dto.response.OcrResult;
import com.Jjambbong.PayLens.document.service.ocr.OcrClient;
import com.Jjambbong.PayLens.document.service.ocr.OcrEngineType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OcrService {

    private final List<OcrClient> ocrClients;

    public OcrResult extractText(Document document, OcrEngineType engineType) {
        return ocrClients.stream()
                .filter(client -> client.getEngineType() == engineType)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 OCR 엔진입니다: " + engineType))
                .extractText(document);
    }

    public OcrComparisonResult compareAll(Document document) {
        List<OcrResult> results = ocrClients.stream()
                .map(client -> extractSafely(client, document))
                .toList();

        String recommendedEngine = results.stream()
                .filter(result -> result.text() != null && !result.text().isBlank())
                .max(Comparator
                        .comparing((OcrResult result) -> result.confidence() == null ? 0.0 : result.confidence())
                        .thenComparing(result -> result.text() == null ? 0 : result.text().length()))
                .map(result -> result.engineType().name())
                .orElse(null);

        return new OcrComparisonResult(
                document.getId(),
                results,
                recommendedEngine
        );
    }

    private OcrResult extractSafely(OcrClient client, Document document) {
        try {
            return client.extractText(document);
        } catch (Exception e) {
            return new OcrResult(
                    client.getEngineType(),
                    "",
                    0.0,
                    null,
                    List.of(new OcrLine("OCR 실패: " + e.getMessage(), 0.0))
            );
        }
    }
}
