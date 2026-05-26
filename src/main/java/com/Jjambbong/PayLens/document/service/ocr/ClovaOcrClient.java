package com.Jjambbong.PayLens.document.service.ocr;

import com.Jjambbong.PayLens.document.domain.Document;
import com.Jjambbong.PayLens.document.dto.response.OcrLine;
import com.Jjambbong.PayLens.document.dto.response.OcrResult;
import com.Jjambbong.PayLens.document.service.AnalyzeService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ClovaOcrClient implements OcrClient {

    private final AnalyzeService analyzeService;

    @Value("${ocr.clova.url:}")
    private String clovaOcrUrl;

    @Value("${ocr.clova.secret:}")
    private String clovaOcrSecret;

    @Override
    public OcrEngineType getEngineType() {
        return OcrEngineType.CLOVA;
    }

    @Override
    public OcrResult extractText(Document document) {
        long start = System.currentTimeMillis();

        if (clovaOcrUrl == null || clovaOcrUrl.isBlank()
                || clovaOcrSecret == null || clovaOcrSecret.isBlank()) {
            long elapsed = System.currentTimeMillis() - start;
            return new OcrResult(
                    OcrEngineType.CLOVA,
                    "",
                    0.0,
                    elapsed,
                    List.of(new OcrLine("Clova OCR URL 또는 Secret이 설정되지 않았습니다.", 0.0))
            );
        }

        byte[] fileBytes = analyzeService.getDocumentBytes(document);
        String base64 = Base64.getEncoder().encodeToString(fileBytes);

        Map<String, Object> requestBody = Map.of(
                "version", "V2",
                "requestId", UUID.randomUUID().toString(),
                "timestamp", System.currentTimeMillis(),
                "images", List.of(Map.of(
                        "format", "jpg",
                        "name", "document",
                        "data", base64
                ))
        );

        Map response = RestClient.builder()
                .build()
                .post()
                .uri(clovaOcrUrl)
                .header("X-OCR-SECRET", clovaOcrSecret)
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        long elapsed = System.currentTimeMillis() - start;

        List<OcrLine> lines = parseClovaLines(response);
        String text = lines.stream()
                .map(OcrLine::text)
                .reduce("", (a, b) -> a + b + "\n");

        return new OcrResult(
                OcrEngineType.CLOVA,
                text,
                null,
                elapsed,
                lines
        );
    }

    @SuppressWarnings("unchecked")
    private List<OcrLine> parseClovaLines(Map response) {
        List<OcrLine> lines = new ArrayList<>();

        if (response == null || !response.containsKey("images")) {
            return lines;
        }

        List<Map<String, Object>> images = (List<Map<String, Object>>) response.get("images");

        for (Map<String, Object> image : images) {
            Object fieldsObj = image.get("fields");

            if (!(fieldsObj instanceof List<?>)) {
                continue;
            }

            List<Map<String, Object>> fields = (List<Map<String, Object>>) fieldsObj;

            for (Map<String, Object> field : fields) {
                Object inferText = field.get("inferText");
                Object inferConfidence = field.get("inferConfidence");

                if (inferText == null) {
                    continue;
                }

                Double confidence = null;
                if (inferConfidence instanceof Number number) {
                    confidence = number.doubleValue();
                }

                lines.add(new OcrLine(inferText.toString(), confidence));
            }
        }

        return lines;
    }
}
