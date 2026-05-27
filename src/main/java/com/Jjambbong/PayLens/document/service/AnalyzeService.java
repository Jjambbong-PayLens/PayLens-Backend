package com.Jjambbong.PayLens.document.service;

import com.Jjambbong.PayLens.document.domain.Document;
import com.Jjambbong.PayLens.global.config.AmazonConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Transactional
public class AnalyzeService {

    private final AmazonConfig amazonConfig;
    private final S3Client s3Client;

    public String getDocumentAsBase64(Document document) {
        ResponseBytes<GetObjectResponse> s3Object = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                .bucket(amazonConfig.getBucket())
                .key(document.getObjectKey())
                .build());

        byte[] pdfBytes = s3Object.asByteArray();
        return Base64.getEncoder().encodeToString(pdfBytes);
    }
}
