package com.Jjambbong.PayLens.document.service.ocr;

import com.Jjambbong.PayLens.document.domain.Document;
import com.Jjambbong.PayLens.document.dto.response.OcrResult;
import com.Jjambbong.PayLens.document.service.AnalyzeService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PaddleOcrClient implements OcrClient {

    private final AnalyzeService analyzeService;

    @Value("${ocr.paddle.url:http://localhost:8001}")
    private String paddleOcrUrl;

    @Override
    public OcrEngineType getEngineType() {
        return OcrEngineType.PADDLE;
    }

    @Override
    public OcrResult extractText(Document document) {
        long start = System.currentTimeMillis();

        byte[] fileBytes = analyzeService.getDocumentBytes(document);

        ByteArrayResource fileResource = new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() {
                return "document";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);

        OcrResult result = RestClient.builder()
                .baseUrl(paddleOcrUrl)
                .build()
                .post()
                .uri("/ocr")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(OcrResult.class);

        long elapsed = System.currentTimeMillis() - start;

        if (result == null) {
            return new OcrResult(
                    OcrEngineType.PADDLE,
                    "",
                    0.0,
                    elapsed,
                    List.of()
            );
        }

        return new OcrResult(
                OcrEngineType.PADDLE,
                result.text(),
                result.confidence(),
                elapsed,
                result.lines()
        );
    }
}
