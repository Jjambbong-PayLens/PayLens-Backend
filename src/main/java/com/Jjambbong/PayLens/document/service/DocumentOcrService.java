package com.Jjambbong.PayLens.document.service;

import com.Jjambbong.PayLens.document.client.OcrAnalyzeFastApiRequest;
import com.Jjambbong.PayLens.document.client.OcrAnalyzeFastApiResult;
import com.Jjambbong.PayLens.document.client.OcrFastApiClient;
import com.Jjambbong.PayLens.document.domain.Document;
import com.Jjambbong.PayLens.document.domain.DocumentOcrStatus;
import com.Jjambbong.PayLens.document.domain.DocumentStatus;
import com.Jjambbong.PayLens.document.dto.request.DocumentOcrRequest;
import com.Jjambbong.PayLens.document.dto.response.DocumentOcrListResponse;
import com.Jjambbong.PayLens.document.dto.response.DocumentOcrResponse;
import com.Jjambbong.PayLens.document.repository.DocumentRepository;
import com.Jjambbong.PayLens.global.api.ErrorCode;
import com.Jjambbong.PayLens.global.config.AmazonConfig;
import com.Jjambbong.PayLens.global.exception.GeneralException;
import com.Jjambbong.PayLens.user.domain.User;
import com.Jjambbong.PayLens.user.repository.UserRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
@Transactional(noRollbackFor = GeneralException.class)
public class DocumentOcrService {

    private static final int MAX_OCR_DOCUMENT_COUNT = 10;
    private static final long OCR_URL_EXPIRATION_SECONDS = 600L;
    private static final String OCR_BASE_PATH = "documents_ocr";
    private static final String OCR_JSON_CONTENT_TYPE = "application/json";

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final AmazonConfig amazonConfig;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final OcrFastApiClient ocrFastApiClient;

    public DocumentOcrListResponse processOcr(Long userId, DocumentOcrRequest request) {
        List<Long> documentIds = validateOcrDocumentIds(request);
        User user = getUser(userId);

        // 문서 목록 순차 OCR 처리
        List<DocumentOcrResponse> responses = documentIds.stream()
                .map(documentId -> processSingleDocument(user, documentId))
                .toList();

        return DocumentOcrListResponse.builder()
                .documents(responses)
                .count(responses.size())
                .build();
    }

    private DocumentOcrResponse processSingleDocument(User user, Long documentId) {
        Document document = findOwnedDocument(user, documentId);
        validateOcrTarget(document);
        document.startOcr();

        try {
            // S3 원본 파일 존재 확인
            verifyS3ObjectExists(document.getObjectKey());

            ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
            String ocrResultKey = buildOcrResultKey(
                    user.getId(),
                    document.getDocumentType().name(),
                    document.getOriginalFileName(),
                    now
            );
            String downloadUrl = createPresignedGetUrl(document.getObjectKey());
            String uploadUrl = createPresignedPutUrl(ocrResultKey, OCR_JSON_CONTENT_TYPE);

            // FastAPI는 downloadUrl로 원본을 다운받고 uploadUrl로 S3에 OCR JSON을 저장
            OcrAnalyzeFastApiResult ocrResult = ocrFastApiClient.analyze(OcrAnalyzeFastApiRequest.builder()
                    .userId(user.getId())
                    .documentId(document.getId())
                    .fileName(document.getOriginalFileName())
                    .contentType(document.getContentType())
                    .documentType(document.getDocumentType().name())
                    .downloadUrl(downloadUrl)
                    .uploadUrl(uploadUrl)
                    .ocrResultKey(ocrResultKey)
                    .build());

            // Presigned URL PUT을 발급한 key와 FastAPI가 보고한 key가 같은지 확인
            if (!ocrResultKey.equals(ocrResult.getOcrResultKey())) {
                throw new GeneralException(ErrorCode.OCR_RESULT_KEY_MISMATCH);
            }

            document.completeOcr(
                    ocrResultKey,
                    ocrResult.getOcrJsonSizeBytes(),
                    parseProcessedAt(ocrResult.getProcessedAt())
            );
            return DocumentOcrResponse.from(document);
        } catch (GeneralException e) {
            document.failOcr(e.getReason().getMessage());
            throw e;
        } catch (RuntimeException e) {
            document.failOcr(ErrorCode.OCR_REQUEST_FAILED.getReason().getMessage());
            throw new GeneralException(ErrorCode.OCR_REQUEST_FAILED);
        }
    }

    private List<Long> validateOcrDocumentIds(DocumentOcrRequest request) {
        if (request == null || request.getDocumentIds() == null
                || request.getDocumentIds().isEmpty()
                || request.getDocumentIds().size() > MAX_OCR_DOCUMENT_COUNT
                || request.getDocumentIds().stream().anyMatch(documentId -> documentId == null)
                || request.getDocumentIds().stream().distinct().count() != request.getDocumentIds().size()) {
            throw new GeneralException(ErrorCode.DOCUMENT_OCR_COUNT_INVALID);
        }
        return request.getDocumentIds();
    }

    private void validateOcrTarget(Document document) {
        if (document.getStatus() != DocumentStatus.UPLOADED) {
            throw new GeneralException(ErrorCode.DOCUMENT_OCR_TARGET_INVALID);
        }
        if (document.getOcrStatus() == DocumentOcrStatus.PROCESSING) {
            throw new GeneralException(ErrorCode.DOCUMENT_OCR_ALREADY_PROCESSING);
        }
    }

    private Document findOwnedDocument(User user, Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new GeneralException(ErrorCode.DOCUMENT_NOT_FOUND));

        if (!document.getUser().getId().equals(user.getId())) {
            throw new GeneralException(ErrorCode.DOCUMENT_ACCESS_DENIED);
        }
        return document;
    }

    private User getUser(Long userId) {
        if (userId == null) {
            throw new GeneralException(ErrorCode.TOKEN_INVALID);
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));
    }

    private String buildOcrResultKey(Long userId, String documentType, String fileName, ZonedDateTime now) {
        // FastAPI와 동일한 규칙으로 OCR JSON 저장 경로를 계산합니다.
        return String.join("/",
                OCR_BASE_PATH,
                String.valueOf(userId),
                documentType,
                String.valueOf(now.getYear()),
                String.format("%02d", now.getMonthValue()),
                fileNameStem(fileName) + "_ocr.json"
        );
    }

    private String fileNameStem(String fileName) {
        String normalized = fileName == null ? "document" : fileName.replace("\\", "/").trim();
        int lastSeparator = normalized.lastIndexOf('/');
        if (lastSeparator >= 0) {
            normalized = normalized.substring(lastSeparator + 1);
        }

        int lastDot = normalized.lastIndexOf('.');
        if (lastDot > 0) {
            normalized = normalized.substring(0, lastDot);
        }

        if (normalized.isBlank() || normalized.equals(".") || normalized.equals("..")) {
            return "document";
        }
        return normalized;
    }

    private String createPresignedGetUrl(String objectKey) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(amazonConfig.getBucket())
                    .key(objectKey)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(OCR_URL_EXPIRATION_SECONDS))
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (RuntimeException e) {
            throw new GeneralException(ErrorCode.PRESIGNED_URL_GENERATION_FAILED);
        }
    }

    private String createPresignedPutUrl(String objectKey, String contentType) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(amazonConfig.getBucket())
                    .key(objectKey)
                    .contentType(contentType)
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(OCR_URL_EXPIRATION_SECONDS))
                    .putObjectRequest(putObjectRequest)
                    .build();

            PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (RuntimeException e) {
            throw new GeneralException(ErrorCode.PRESIGNED_URL_GENERATION_FAILED);
        }
    }

    private void verifyS3ObjectExists(String objectKey) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(amazonConfig.getBucket())
                    .key(objectKey)
                    .build());
        } catch (AwsServiceException e) {
            if (e.statusCode() == 404) {
                throw new GeneralException(ErrorCode.DOCUMENT_OBJECT_NOT_FOUND);
            }
            throw new GeneralException(ErrorCode.S3_UPLOAD_FAILED);
        } catch (SdkClientException e) {
            throw new GeneralException(ErrorCode.S3_UPLOAD_FAILED);
        }
    }

    private LocalDateTime parseProcessedAt(String processedAt) {
        if (processedAt == null || processedAt.isBlank()) {
            return LocalDateTime.now(ZoneOffset.UTC);
        }
        try {
            return OffsetDateTime.parse(processedAt).toLocalDateTime();
        } catch (RuntimeException e) {
            return LocalDateTime.now(ZoneOffset.UTC);
        }
    }
}
