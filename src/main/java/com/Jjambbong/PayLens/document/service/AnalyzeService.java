package com.Jjambbong.PayLens.document.service;

import com.Jjambbong.PayLens.document.domain.Document;
import com.Jjambbong.PayLens.global.config.AmazonConfig;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@Service
@RequiredArgsConstructor
@Transactional
public class AnalyzeService {

    private final AmazonConfig amazonConfig;
    private final S3Client s3Client;

    public String getDocumentAsBase64(Document document) {
        return Base64.getEncoder().encodeToString(getObjectBytes(document.getObjectKey()));
    }

    public String getOcrJson(Document document) {
        return new String(getObjectBytes(document.getOcrResultKey()), StandardCharsets.UTF_8);
    }

    private byte[] getObjectBytes(String objectKey) {
        ResponseBytes<GetObjectResponse> s3Object = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                .bucket(amazonConfig.getBucket())
                .key(objectKey)
                .build());

        return s3Object.asByteArray();
    }
}
