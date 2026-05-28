package com.Jjambbong.PayLens.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.Jjambbong.PayLens.document.client.OcrAnalyzeFastApiRequest;
import com.Jjambbong.PayLens.document.client.OcrAnalyzeFastApiResult;
import com.Jjambbong.PayLens.document.client.OcrFastApiClient;
import com.Jjambbong.PayLens.document.domain.Document;
import com.Jjambbong.PayLens.document.domain.DocumentOcrStatus;
import com.Jjambbong.PayLens.document.domain.DocumentStatus;
import com.Jjambbong.PayLens.document.domain.DocumentType;
import com.Jjambbong.PayLens.document.dto.request.DocumentOcrRequest;
import com.Jjambbong.PayLens.document.dto.response.DocumentOcrListResponse;
import com.Jjambbong.PayLens.document.repository.DocumentRepository;
import com.Jjambbong.PayLens.global.api.ErrorCode;
import com.Jjambbong.PayLens.global.config.AmazonConfig;
import com.Jjambbong.PayLens.global.exception.GeneralException;
import com.Jjambbong.PayLens.user.domain.User;
import com.Jjambbong.PayLens.user.domain.UserRole;
import com.Jjambbong.PayLens.user.domain.UserStatus;
import com.Jjambbong.PayLens.user.repository.UserRepository;
import java.net.MalformedURLException;
import java.net.URI;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@ExtendWith(MockitoExtension.class)
class DocumentOcrServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AmazonConfig amazonConfig;

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private OcrFastApiClient ocrFastApiClient;

    private DocumentOcrService documentOcrService;

    @BeforeEach
    void setUp() {
        documentOcrService = new DocumentOcrService(
                documentRepository,
                userRepository,
                amazonConfig,
                s3Client,
                s3Presigner,
                ocrFastApiClient
        );
    }

    @Test
    void processesOcrAndStoresResultMetadata() throws MalformedURLException {
        User user = user(1L);
        Document document = uploadedDocument(user, 10L, "paystub.pdf");
        String expectedKey = expectedOcrKey(user.getId(), document.getDocumentType().name(), "paystub");
        PresignedGetObjectRequest getRequest = mock(PresignedGetObjectRequest.class);
        PresignedPutObjectRequest putRequest = mock(PresignedPutObjectRequest.class);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(documentRepository.findById(10L)).thenReturn(Optional.of(document));
        when(amazonConfig.getBucket()).thenReturn("paylens-dev-documents");
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(HeadObjectResponse.builder().build());
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(getRequest);
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(putRequest);
        when(getRequest.url()).thenReturn(URI.create("https://example.com/download").toURL());
        when(putRequest.url()).thenReturn(URI.create("https://example.com/upload").toURL());
        when(ocrFastApiClient.analyze(any(OcrAnalyzeFastApiRequest.class)))
                .thenReturn(ocrResult(expectedKey, 12345L));

        DocumentOcrListResponse response = documentOcrService.processOcr(
                1L,
                new DocumentOcrRequest(List.of(10L))
        );

        assertThat(response.getCount()).isEqualTo(1);
        assertThat(response.getDocuments().get(0).getOcrStatus()).isEqualTo(DocumentOcrStatus.COMPLETED);
        assertThat(response.getDocuments().get(0).getOcrResultKey()).isEqualTo(expectedKey);
        assertThat(document.getOcrStatus()).isEqualTo(DocumentOcrStatus.COMPLETED);
        assertThat(document.getOcrResultKey()).isEqualTo(expectedKey);
        assertThat(document.getOcrJsonSizeBytes()).isEqualTo(12345L);

        ArgumentCaptor<OcrAnalyzeFastApiRequest> captor = ArgumentCaptor.forClass(OcrAnalyzeFastApiRequest.class);
        verify(ocrFastApiClient).analyze(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(1L);
        assertThat(captor.getValue().getDocumentId()).isEqualTo(10L);
        assertThat(captor.getValue().getFileName()).isEqualTo("paystub.pdf");
        assertThat(captor.getValue().getContentType()).isEqualTo("application/pdf");
        assertThat(captor.getValue().getDocumentType()).isEqualTo("PAYSLIP");
        assertThat(captor.getValue().getDownloadUrl()).isEqualTo("https://example.com/download");
        assertThat(captor.getValue().getUploadUrl()).isEqualTo("https://example.com/upload");
        assertThat(captor.getValue().getOcrResultKey()).isEqualTo(expectedKey);
    }

    @Test
    void rejectsOcrWhenDocumentIdsAreInvalid() {
        List<Long> documentIds = LongStream.rangeClosed(1L, 11L)
                .boxed()
                .toList();

        assertThatThrownBy(() -> documentOcrService.processOcr(1L, new DocumentOcrRequest(documentIds)))
                .isInstanceOfSatisfying(GeneralException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(ErrorCode.DOCUMENT_OCR_COUNT_INVALID));
    }

    @Test
    void rejectsOcrWhenDocumentIsNotUploaded() {
        User user = user(1L);
        Document document = readyDocument(user, 10L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(documentRepository.findById(10L)).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> documentOcrService.processOcr(1L, new DocumentOcrRequest(List.of(10L))))
                .isInstanceOfSatisfying(GeneralException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(ErrorCode.DOCUMENT_OCR_TARGET_INVALID));
    }

    @Test
    void rejectsOcrWhenDocumentIsAlreadyProcessing() {
        User user = user(1L);
        Document document = uploadedDocument(user, 10L, "paystub.pdf");
        document.startOcr();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(documentRepository.findById(10L)).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> documentOcrService.processOcr(1L, new DocumentOcrRequest(List.of(10L))))
                .isInstanceOfSatisfying(GeneralException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(ErrorCode.DOCUMENT_OCR_ALREADY_PROCESSING));
    }

    @Test
    void failsOcrWhenFastApiReturnsDifferentResultKey() throws MalformedURLException {
        User user = user(1L);
        Document document = uploadedDocument(user, 10L, "paystub.pdf");
        PresignedGetObjectRequest getRequest = mock(PresignedGetObjectRequest.class);
        PresignedPutObjectRequest putRequest = mock(PresignedPutObjectRequest.class);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(documentRepository.findById(10L)).thenReturn(Optional.of(document));
        when(amazonConfig.getBucket()).thenReturn("paylens-dev-documents");
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(HeadObjectResponse.builder().build());
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(getRequest);
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(putRequest);
        when(getRequest.url()).thenReturn(URI.create("https://example.com/download").toURL());
        when(putRequest.url()).thenReturn(URI.create("https://example.com/upload").toURL());
        when(ocrFastApiClient.analyze(any(OcrAnalyzeFastApiRequest.class)))
                .thenReturn(ocrResult("documents_ocr/1/PAYSLIP/2026/05/other_ocr.json", 12345L));

        assertThatThrownBy(() -> documentOcrService.processOcr(1L, new DocumentOcrRequest(List.of(10L))))
                .isInstanceOfSatisfying(GeneralException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(ErrorCode.OCR_RESULT_KEY_MISMATCH));
        assertThat(document.getOcrStatus()).isEqualTo(DocumentOcrStatus.FAILED);
    }

    private String expectedOcrKey(Long userId, String documentType, String stem) {
        YearMonth now = YearMonth.now(ZoneOffset.UTC);
        return "documents_ocr/%d/%s/%d/%02d/%s_ocr.json".formatted(
                userId,
                documentType,
                now.getYear(),
                now.getMonthValue(),
                stem
        );
    }

    private OcrAnalyzeFastApiResult ocrResult(String ocrResultKey, Long size) {
        OcrAnalyzeFastApiResult result = new OcrAnalyzeFastApiResult();
        ReflectionTestUtils.setField(result, "ocrResultKey", ocrResultKey);
        ReflectionTestUtils.setField(result, "ocrJsonSizeBytes", size);
        ReflectionTestUtils.setField(result, "processedAt", "2026-05-28T12:00:00Z");
        return result;
    }

    private User user(Long id) {
        User user = User.builder()
                .providerId("provider-" + id)
                .email("user" + id + "@example.com")
                .username("user" + id)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "Id", id);
        return user;
    }

    private Document uploadedDocument(User user, Long id, String fileName) {
        Document document = readyDocument(user, id);
        ReflectionTestUtils.setField(document, "originalFileName", fileName);
        document.completeUpload();
        return document;
    }

    private Document readyDocument(User user, Long id) {
        Document document = Document.builder()
                .user(user)
                .originalFileName("paystub.pdf")
                .storedFileName("uuid_paystub.pdf")
                .objectKey("documents/" + user.getId() + "/PAYSLIP/2026/04/uuid_paystub.pdf")
                .contentType("application/pdf")
                .documentType(DocumentType.PAYSLIP)
                .status(DocumentStatus.UPLOAD_READY)
                .build();
        ReflectionTestUtils.setField(document, "id", id);
        return document;
    }
}
