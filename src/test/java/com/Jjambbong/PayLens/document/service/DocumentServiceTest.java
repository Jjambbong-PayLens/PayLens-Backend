package com.Jjambbong.PayLens.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.Jjambbong.PayLens.document.domain.Document;
import com.Jjambbong.PayLens.document.domain.DocumentStatus;
import com.Jjambbong.PayLens.document.domain.DocumentType;
import com.Jjambbong.PayLens.document.dto.request.DocumentCompleteRequest;
import com.Jjambbong.PayLens.document.dto.request.DocumentDeleteRequest;
import com.Jjambbong.PayLens.document.dto.request.DocumentUploadUrlRequest;
import com.Jjambbong.PayLens.document.dto.request.DocumentUploadUrlsRequest;
import com.Jjambbong.PayLens.document.dto.response.DocumentCompleteListResponse;
import com.Jjambbong.PayLens.document.dto.response.DocumentCompleteResponse;
import com.Jjambbong.PayLens.document.dto.response.DocumentDeleteListResponse;
import com.Jjambbong.PayLens.document.dto.response.DocumentListResponse;
import com.Jjambbong.PayLens.document.dto.response.DocumentUploadUrlsResponse;
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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

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

    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        documentService = new DocumentService(
                documentRepository,
                userRepository,
                new DocumentUploadPolicy(),
                amazonConfig,
                s3Client,
                s3Presigner
        );
    }

    @Test
    void getsUploadedDocumentsForUser() {
        User user = user(1L);
        Document payslip = document(user);
        payslip.completeUpload();
        Document contract = document(user, 11L, DocumentType.EMPLOYMENT_CONTRACT);
        contract.completeUpload();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(documentRepository.findByUserAndStatusOrderByCreatedAtDesc(user, DocumentStatus.UPLOADED))
                .thenReturn(List.of(payslip, contract));

        DocumentListResponse response = documentService.getUploadedDocuments(1L);

        assertThat(response.getCount()).isEqualTo(2);
        assertThat(response.getDocuments()).extracting("documentId").containsExactly(10L, 11L);
        assertThat(response.getDocuments()).allSatisfy(document ->
                assertThat(document.getStatus()).isEqualTo(DocumentStatus.UPLOADED));
    }

    @Test
    void deletesDocumentsFromS3AndDatabase() {
        User user = user(1L);
        Document payslip = document(user);
        Document contract = document(user, 11L, DocumentType.EMPLOYMENT_CONTRACT);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(documentRepository.findById(10L)).thenReturn(Optional.of(payslip));
        when(documentRepository.findById(11L)).thenReturn(Optional.of(contract));
        when(amazonConfig.getBucket()).thenReturn("paylens-dev-documents");
        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(DeleteObjectResponse.builder().build());

        payslip.completeOcr("documents_ocr/1/PAYSLIP/2026/05/paystub_ocr.json", 100L, java.time.LocalDateTime.now());

        DocumentDeleteListResponse response = documentService.deleteDocuments(
                1L,
                new DocumentDeleteRequest(List.of(10L, 11L))
        );

        assertThat(response.getCount()).isEqualTo(2);
        assertThat(response.getDocuments()).extracting("documentId").containsExactly(10L, 11L);
        verify(s3Client, times(3)).deleteObject(any(DeleteObjectRequest.class));
        verify(documentRepository).deleteAll(List.of(payslip, contract));
    }

    @Test
    void rejectsDeleteBatchWhenDocumentCountExceedsLimit() {
        User user = user(1L);
        List<Long> documentIds = java.util.stream.LongStream.rangeClosed(1L, 11L)
                .boxed()
                .toList();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> documentService.deleteDocuments(1L, new DocumentDeleteRequest(documentIds)))
                .isInstanceOfSatisfying(GeneralException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(ErrorCode.DOCUMENT_DELETE_COUNT_INVALID));
    }

    @Test
    void createsUploadUrlsForMultipleFiles() throws MalformedURLException {
        User user = user(1L);
        DocumentUploadUrlsRequest request = new DocumentUploadUrlsRequest(List.of(
                new DocumentUploadUrlRequest("paystub.pdf", "application/pdf", "PAYSLIP"),
                new DocumentUploadUrlRequest("contract.docx",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "EMPLOYMENT_CONTRACT")
        ));
        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(amazonConfig.getLocationPath()).thenReturn("documents");
        when(amazonConfig.getBucket()).thenReturn("paylens-dev-documents");
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document document = invocation.getArgument(0);
            ReflectionTestUtils.setField(document, "id", 100L + document.getDocumentType().ordinal());
            return document;
        });
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presignedRequest);
        when(presignedRequest.url()).thenReturn(URI.create("https://example.com/upload").toURL());

        DocumentUploadUrlsResponse response = documentService.createUploadUrls(1L, request);

        assertThat(response.getCount()).isEqualTo(2);
        assertThat(response.getFiles()).hasSize(2);
        assertThat(response.getFiles())
                .allSatisfy(file -> {
                    assertThat(file.getUploadUrl()).isEqualTo("https://example.com/upload");
                    assertThat(file.getExpiresInSeconds()).isEqualTo(600L);
                    assertThat(file.getRequiredHeaders()).containsKey("Content-Type");
                });
        verify(documentRepository, times(2)).save(any(Document.class));
        verify(s3Presigner, times(2)).presignPutObject(any(PutObjectPresignRequest.class));
    }

    @Test
    void rejectsUploadUrlBatchWhenFileCountExceedsLimit() {
        User user = user(1L);
        List<DocumentUploadUrlRequest> files = java.util.stream.IntStream.range(0, 11)
                .mapToObj(index -> new DocumentUploadUrlRequest(
                        "paystub-" + index + ".pdf",
                        "application/pdf",
                        "PAYSLIP"))
                .toList();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> documentService.createUploadUrls(1L, new DocumentUploadUrlsRequest(files)))
                .isInstanceOfSatisfying(GeneralException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(ErrorCode.DOCUMENT_UPLOAD_FILE_COUNT_INVALID));
    }

    @Test
    void completesMultipleUploadsWhenS3ObjectsExist() {
        User user = user(1L);
        Document payslip = document(user);
        Document contract = document(user, 11L, DocumentType.EMPLOYMENT_CONTRACT);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(documentRepository.findById(10L)).thenReturn(Optional.of(payslip));
        when(documentRepository.findById(11L)).thenReturn(Optional.of(contract));
        when(amazonConfig.getBucket()).thenReturn("paylens-dev-documents");
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(HeadObjectResponse.builder().build());

        DocumentCompleteListResponse response = documentService.completeUploads(
                1L,
                new DocumentCompleteRequest(List.of(10L, 11L))
        );

        assertThat(response.getCount()).isEqualTo(2);
        assertThat(response.getDocuments()).hasSize(2);
        assertThat(payslip.getStatus()).isEqualTo(DocumentStatus.UPLOADED);
        assertThat(contract.getStatus()).isEqualTo(DocumentStatus.UPLOADED);
        verify(s3Client, times(2)).headObject(any(HeadObjectRequest.class));
    }

    @Test
    void rejectsCompleteBatchWhenDocumentCountExceedsLimit() {
        User user = user(1L);
        List<Long> documentIds = java.util.stream.LongStream.rangeClosed(1L, 11L)
                .boxed()
                .toList();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> documentService.completeUploads(1L, new DocumentCompleteRequest(documentIds)))
                .isInstanceOfSatisfying(GeneralException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(ErrorCode.DOCUMENT_COMPLETE_COUNT_INVALID));
    }

    @Test
    void completesUploadWhenS3ObjectExists() {
        User user = user(1L);
        Document document = document(user);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(documentRepository.findById(10L)).thenReturn(Optional.of(document));
        when(amazonConfig.getBucket()).thenReturn("paylens-dev-documents");
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(HeadObjectResponse.builder().build());

        DocumentCompleteResponse response = documentService.completeUpload(1L, 10L);

        assertThat(document.getStatus()).isEqualTo(DocumentStatus.UPLOADED);
        assertThat(response.getStatus()).isEqualTo(DocumentStatus.UPLOADED);
        assertThat(response.getDocumentType()).isEqualTo(DocumentType.PAYSLIP);
    }

    @Test
    void rejectsCompletionByDifferentUser() {
        User requester = user(1L);
        User owner = user(2L);
        Document document = document(owner);

        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(documentRepository.findById(10L)).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> documentService.completeUpload(1L, 10L))
                .isInstanceOfSatisfying(GeneralException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(ErrorCode.DOCUMENT_ACCESS_DENIED));
    }

    @Test
    void doesNotCompleteWhenS3ObjectDoesNotExist() {
        User user = user(1L);
        Document document = document(user);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(documentRepository.findById(10L)).thenReturn(Optional.of(document));
        when(amazonConfig.getBucket()).thenReturn("paylens-dev-documents");
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(
                S3Exception.builder().statusCode(404).message("not found").build()
        );

        assertThatThrownBy(() -> documentService.completeUpload(1L, 10L))
                .isInstanceOfSatisfying(GeneralException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(ErrorCode.DOCUMENT_OBJECT_NOT_FOUND));
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.UPLOAD_READY);
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

    private Document document(User user) {
        Document document = Document.builder()
                .user(user)
                .originalFileName("paystub.pdf")
                .storedFileName("uuid_paystub.pdf")
                .objectKey("documents/" + user.getId() + "/PAYSLIP/2026/04/uuid_paystub.pdf")
                .contentType("application/pdf")
                .documentType(DocumentType.PAYSLIP)
                .status(DocumentStatus.UPLOAD_READY)
                .build();
        ReflectionTestUtils.setField(document, "id", 10L);
        return document;
    }

    private Document document(User user, Long id, DocumentType documentType) {
        Document document = Document.builder()
                .user(user)
                .originalFileName(documentType.name().toLowerCase() + ".pdf")
                .storedFileName("uuid_" + documentType.name().toLowerCase() + ".pdf")
                .objectKey("documents/" + user.getId() + "/" + documentType.name() + "/2026/04/uuid.pdf")
                .contentType("application/pdf")
                .documentType(documentType)
                .status(DocumentStatus.UPLOAD_READY)
                .build();
        ReflectionTestUtils.setField(document, "id", id);
        return document;
    }
}
