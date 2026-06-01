package com.Jjambbong.PayLens.document.client;

import com.Jjambbong.PayLens.global.api.ErrorCode;
import com.Jjambbong.PayLens.global.exception.GeneralException;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class OcrFastApiClient {

    private final RestClient restClient;
    private final String internalToken;

    public OcrFastApiClient(
            @Value("${ocr.fastapi.base-url:http://127.0.0.1:8000}") String baseUrl,
            @Value("${ocr.fastapi.internal-token:}") String internalToken,
            @Value("${ocr.fastapi.timeout-seconds:180}") long timeoutSeconds
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofSeconds(timeoutSeconds);
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);

        this.restClient = RestClient.builder()
                .baseUrl(normalizeBaseUrl(baseUrl))
                .requestFactory(requestFactory)
                .build();
        this.internalToken = internalToken;
    }

    public OcrAnalyzeFastApiResult analyze(OcrAnalyzeFastApiRequest request) {
        try {
            // OCR 서버는 내부 API이므로 X-Internal-Token으로 호출을 보호
            OcrAnalyzeFastApiResponse response = restClient.post()
                    .uri("/ocr/analyze")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Internal-Token", internalToken)
                    .body(request)
                    .retrieve()
                    .body(OcrAnalyzeFastApiResponse.class);

            // OCR JSON S3 업로드 완료 응답(OCR_2003)만 성공으로 처리
            if (response == null || !response.isSuccessfulUploadResponse()) {
                throw new GeneralException(ErrorCode.OCR_RESPONSE_INVALID);
            }
            return response.getResult();
        } catch (GeneralException e) {
            throw e;
        } catch (RestClientException e) {
            throw new GeneralException(ErrorCode.OCR_REQUEST_FAILED);
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://127.0.0.1:8000";
        }
        return baseUrl.replaceAll("/+$", "");
    }
}
