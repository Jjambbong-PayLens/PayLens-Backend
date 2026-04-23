package com.Jjambbong.PayLens.document.service;

import com.Jjambbong.PayLens.document.domain.Document;
import com.Jjambbong.PayLens.document.domain.DocumentStatus;
import com.Jjambbong.PayLens.document.dto.request.DocumentUploadUrlRequest;
import com.Jjambbong.PayLens.document.dto.response.DocumentCompleteResponse;
import com.Jjambbong.PayLens.document.dto.response.DocumentUploadUrlResponse;
import com.Jjambbong.PayLens.document.repository.DocumentRepository;
import com.Jjambbong.PayLens.global.api.ErrorCode;
import com.Jjambbong.PayLens.global.config.AmazonConfig;
import com.Jjambbong.PayLens.global.exception.GeneralException;
import com.Jjambbong.PayLens.user.domain.User;
import com.Jjambbong.PayLens.user.repository.UserRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentService {

    private static final long UPLOAD_URL_EXPIRATION_SECONDS = 600L;
    private static final String DEFAULT_BASE_PATH = "documents";

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final DocumentUploadPolicy documentUploadPolicy;
    private final AmazonConfig amazonConfig;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    public DocumentUploadUrlResponse createUploadUrl(Long userId, DocumentUploadUrlRequest request) {
        User user = getUser(userId);
        DocumentUploadFile uploadFile = documentUploadPolicy.validate(request);

        String storedFileName = UUID.randomUUID() + "_" + uploadFile.sanitizedFileName();
        String objectKey = buildObjectKey(user.getId(), uploadFile.documentType().name(), storedFileName);

        Document document = documentRepository.save(Document.builder()
                .user(user)
                .originalFileName(uploadFile.originalFileName())
                .storedFileName(storedFileName)
                .objectKey(objectKey)
                .contentType(uploadFile.contentType())
                .documentType(uploadFile.documentType())
                .status(DocumentStatus.UPLOAD_READY)
                .build());

        String uploadUrl = createPresignedPutUrl(objectKey, uploadFile.contentType());

        return DocumentUploadUrlResponse.builder()
                .documentId(document.getId())
                .uploadUrl(uploadUrl)
                .objectKey(objectKey)
                .expiresInSeconds(UPLOAD_URL_EXPIRATION_SECONDS)
                .requiredHeaders(Map.of("Content-Type", uploadFile.contentType()))
                .build();
    }

    public DocumentCompleteResponse completeUpload(Long userId, Long documentId) {
        User user = getUser(userId);
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new GeneralException(ErrorCode.DOCUMENT_NOT_FOUND));

        if (!document.getUser().getId().equals(user.getId())) {
            throw new GeneralException(ErrorCode.DOCUMENT_ACCESS_DENIED);
        }

        if (document.getStatus() != DocumentStatus.UPLOAD_READY) {
            throw new GeneralException(ErrorCode.DOCUMENT_UPLOAD_NOT_READY);
        }

        verifyS3ObjectExists(document.getObjectKey());
        document.completeUpload();

        return DocumentCompleteResponse.from(document);
    }

    private User getUser(Long userId) {
        if (userId == null) {
            throw new GeneralException(ErrorCode.TOKEN_INVALID);
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));
    }

    private String buildObjectKey(Long userId, String documentType, String storedFileName) {
        LocalDate now = LocalDate.now();
        return String.join("/",
                normalizeBasePath(),
                String.valueOf(userId),
                documentType,
                String.valueOf(now.getYear()),
                String.format("%02d", now.getMonthValue()),
                storedFileName
        );
    }

    private String normalizeBasePath() {
        String locationPath = amazonConfig.getLocationPath();
        if (locationPath == null || locationPath.isBlank() || locationPath.equalsIgnoreCase("none")) {
            return DEFAULT_BASE_PATH;
        }
        return locationPath.replaceAll("^/+|/+$", "");
    }

    private String createPresignedPutUrl(String objectKey, String contentType) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(amazonConfig.getBucket())
                    .key(objectKey)
                    .contentType(contentType)
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(UPLOAD_URL_EXPIRATION_SECONDS))
                    .putObjectRequest(putObjectRequest)
                    .build();

            PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (AwsServiceException | SdkClientException e) {
            throw new GeneralException(ErrorCode.PRESIGNED_URL_GENERATION_FAILED);
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
}
