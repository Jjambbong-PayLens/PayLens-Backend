package com.Jjambbong.PayLens.document.service;

import com.Jjambbong.PayLens.document.domain.Analysis;
import com.Jjambbong.PayLens.document.domain.Document;
import com.Jjambbong.PayLens.document.domain.DocumentStatus;
import com.Jjambbong.PayLens.document.dto.request.DocumentCompleteRequest;
import com.Jjambbong.PayLens.document.dto.request.DocumentDeleteRequest;
import com.Jjambbong.PayLens.document.dto.request.DocumentUploadUrlRequest;
import com.Jjambbong.PayLens.document.dto.request.DocumentUploadUrlsRequest;
import com.Jjambbong.PayLens.document.dto.response.*;
import com.Jjambbong.PayLens.document.repository.AnalysisRepository;
import com.Jjambbong.PayLens.document.repository.DocumentRepository;
import com.Jjambbong.PayLens.global.api.ErrorCode;
import com.Jjambbong.PayLens.global.config.AmazonConfig;
import com.Jjambbong.PayLens.global.exception.GeneralException;
import com.Jjambbong.PayLens.user.domain.User;
import com.Jjambbong.PayLens.user.repository.UserRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
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
    private static final int MAX_UPLOAD_FILE_COUNT = 10;
    private static final int MAX_COMPLETE_DOCUMENT_COUNT = 10;
    private static final int MAX_DELETE_DOCUMENT_COUNT = 10;

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final AnalysisRepository analysisRepository;
    private final DocumentUploadPolicy documentUploadPolicy;
    private final AmazonConfig amazonConfig;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DocumentUploadUrlResponse createUploadUrl(Long userId, DocumentUploadUrlRequest request) {
        User user = getUser(userId);
        return createUploadUrl(user, request);
    }

    public DocumentUploadUrlsResponse createUploadUrls(Long userId, DocumentUploadUrlsRequest request) {
        User user = getUser(userId);
        List<DocumentUploadUrlRequest> files = validateUploadFiles(request);

        List<DocumentUploadUrlResponse> responses = files.stream()
                .map(file -> createUploadUrl(user, file))
                .toList();

        return DocumentUploadUrlsResponse.builder()
                .files(responses)
                .count(responses.size())
                .build();
    }

    private DocumentUploadUrlResponse createUploadUrl(User user, DocumentUploadUrlRequest request) {
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

    private List<DocumentUploadUrlRequest> validateUploadFiles(DocumentUploadUrlsRequest request) {
        if (request == null || request.getFiles() == null
                || request.getFiles().isEmpty()
                || request.getFiles().size() > MAX_UPLOAD_FILE_COUNT) {
            throw new GeneralException(ErrorCode.DOCUMENT_UPLOAD_FILE_COUNT_INVALID);
        }
        return request.getFiles();
    }

    public DocumentCompleteResponse completeUpload(Long userId, Long documentId) {
        User user = getUser(userId);
        return completeUpload(user, documentId);
    }

    public DocumentCompleteListResponse completeUploads(Long userId, DocumentCompleteRequest request) {
        User user = getUser(userId);
        List<Long> documentIds = validateCompleteDocumentIds(request);

        List<DocumentCompleteResponse> responses = documentIds.stream()
                .map(documentId -> completeUpload(user, documentId))
                .toList();

        return DocumentCompleteListResponse.builder()
                .documents(responses)
                .count(responses.size())
                .build();
    }

    @Transactional(readOnly = true)
    public DocumentListResponse getUploadedDocuments(Long userId) {
        User user = getUser(userId);
        List<DocumentListItemResponse> documents = documentRepository
                .findByUserAndStatusOrderByCreatedAtDesc(user, DocumentStatus.UPLOADED)
                .stream()
                .map(DocumentListItemResponse::from)
                .toList();

        return DocumentListResponse.builder()
                .documents(documents)
                .count(documents.size())
                .build();
    }

    public DocumentDeleteListResponse deleteDocuments(Long userId, DocumentDeleteRequest request) {
        User user = getUser(userId);
        List<Long> documentIds = validateDeleteDocumentIds(request);
        List<Document> documents = documentIds.stream()
                .map(documentId -> findOwnedDocument(user, documentId))
                .toList();
        List<DocumentDeleteResponse> responses = documents.stream()
                .map(DocumentDeleteResponse::from)
                .toList();

        documents.forEach(document -> deleteS3Object(document.getObjectKey()));
        documentRepository.deleteAll(documents);

        return DocumentDeleteListResponse.builder()
                .documents(responses)
                .count(responses.size())
                .build();
    }

    private DocumentCompleteResponse completeUpload(User user, Long documentId) {
        Document document = findOwnedDocument(user, documentId);

        if (document.getStatus() != DocumentStatus.UPLOAD_READY) {
            throw new GeneralException(ErrorCode.DOCUMENT_UPLOAD_NOT_READY);
        }

        verifyS3ObjectExists(document.getObjectKey());
        document.completeUpload();

        return DocumentCompleteResponse.from(document);
    }

    private List<Long> validateCompleteDocumentIds(DocumentCompleteRequest request) {
        if (request == null || request.getDocumentIds() == null
                || request.getDocumentIds().isEmpty()
                || request.getDocumentIds().size() > MAX_COMPLETE_DOCUMENT_COUNT) {
            throw new GeneralException(ErrorCode.DOCUMENT_COMPLETE_COUNT_INVALID);
        }
        return request.getDocumentIds();
    }

    private List<Long> validateDeleteDocumentIds(DocumentDeleteRequest request) {
        if (request == null || request.getDocumentIds() == null
                || request.getDocumentIds().isEmpty()
                || request.getDocumentIds().size() > MAX_DELETE_DOCUMENT_COUNT
                || request.getDocumentIds().stream().anyMatch(documentId -> documentId == null)
                || request.getDocumentIds().stream().distinct().count() != request.getDocumentIds().size()) {
            throw new GeneralException(ErrorCode.DOCUMENT_DELETE_COUNT_INVALID);
        }
        return request.getDocumentIds();
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

    private void deleteS3Object(String objectKey) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(amazonConfig.getBucket())
                    .key(objectKey)
                    .build());
        } catch (AwsServiceException | SdkClientException e) {
            throw new GeneralException(ErrorCode.S3_DELETE_FAILED);
        }
    }

    @Transactional(readOnly = true)
    public AnalysisResponse getAnalysisResult(Long userId, Long documentId) {
        User user = getUser(userId);
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new GeneralException(ErrorCode.DOCUMENT_NOT_FOUND));

        if (!document.getUser().getId().equals(user.getId())) {
            throw new GeneralException(ErrorCode.DOCUMENT_ACCESS_DENIED);
        }

        Analysis analysis = document.getAnalysis();

        if (analysis == null) {
            throw new GeneralException(ErrorCode.ANALYSIS_NOT_FOUND);
        }

        return new AnalysisResponse(analysis, objectMapper);
    }

    @Transactional(readOnly = true)
    public List<AnalysisListItemResponse> getMyAnalyses(Long userId) {
        User user = getUser(userId);
        List<Analysis> analyses = analysisRepository.findByUserOrderByCreatedAtDesc(user);
        return analyses.stream()
                .map(AnalysisListItemResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AnalysisResponse getAnalysisResultByAnalysisId(Long userId, Long analysisId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new GeneralException(ErrorCode.ANALYSIS_NOT_FOUND));

        // 분석 결과의 소유자가 요청한 사용자가 맞는지 확인
        if (!analysis.getUser().getId().equals(user.getId())) {
            throw new GeneralException(ErrorCode.DOCUMENT_ACCESS_DENIED);
        }

        return new AnalysisResponse(analysis, objectMapper);
    }

    @Transactional
    public void deleteAnalysis(Long userId, Long analysisId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new GeneralException(ErrorCode.ANALYSIS_NOT_FOUND));

        // 분석 결과의 소유자가 요청한 사용자가 맞는지 확인
        if (!analysis.getUser().getId().equals(user.getId())) {
            throw new GeneralException(ErrorCode.DOCUMENT_ACCESS_DENIED);
        }

        // [핵심] 연관된 문서들의 analysis_id를 null로 업데이트 (연관관계 끊기)
        for (Document document : analysis.getDocuments()) {
            document.setAnalysis(null);
        }

        // 분석 결과 삭제
        analysisRepository.delete(analysis);
    }
}
