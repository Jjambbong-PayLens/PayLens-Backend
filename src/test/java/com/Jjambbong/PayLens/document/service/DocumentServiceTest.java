package com.Jjambbong.PayLens.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.Jjambbong.PayLens.document.domain.Document;
import com.Jjambbong.PayLens.document.domain.DocumentStatus;
import com.Jjambbong.PayLens.document.domain.DocumentType;
import com.Jjambbong.PayLens.document.dto.response.DocumentCompleteResponse;
import com.Jjambbong.PayLens.document.repository.DocumentRepository;
import com.Jjambbong.PayLens.global.api.ErrorCode;
import com.Jjambbong.PayLens.global.config.AmazonConfig;
import com.Jjambbong.PayLens.global.exception.GeneralException;
import com.Jjambbong.PayLens.user.domain.User;
import com.Jjambbong.PayLens.user.domain.UserRole;
import com.Jjambbong.PayLens.user.domain.UserStatus;
import com.Jjambbong.PayLens.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

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
}
