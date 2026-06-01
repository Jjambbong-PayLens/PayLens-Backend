package com.Jjambbong.PayLens.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.Jjambbong.PayLens.document.domain.Document;
import com.Jjambbong.PayLens.document.domain.DocumentStatus;
import com.Jjambbong.PayLens.document.domain.DocumentType;
import com.Jjambbong.PayLens.document.dto.request.DocumentAnalyzeRequest;
import com.Jjambbong.PayLens.document.repository.AnalysisRepository;
import com.Jjambbong.PayLens.document.repository.DocumentRepository;
import com.Jjambbong.PayLens.global.api.ErrorCode;
import com.Jjambbong.PayLens.global.exception.GeneralException;
import com.Jjambbong.PayLens.survey.repository.SurveyRepository;
import com.Jjambbong.PayLens.survey.service.SurveyService;
import com.Jjambbong.PayLens.user.domain.User;
import com.Jjambbong.PayLens.user.domain.UserRole;
import com.Jjambbong.PayLens.user.domain.UserStatus;
import com.Jjambbong.PayLens.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GeminiServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private AnalyzeService analyzeService;

    @Mock
    private SurveyRepository surveyRepository;

    @Mock
    private AnalysisRepository analysisRepository;

    private GeminiService geminiService;

    @BeforeEach
    void setUp() {
        geminiService = new GeminiService(userRepository, documentRepository, surveyRepository, analysisRepository, analyzeService);
    }

    @Test
    void rejectsAnalyzeWhenDocumentIdsAreNull() {
        assertAnalyzeCountInvalid(new DocumentAnalyzeRequest(null));
    }

    @Test
    void rejectsAnalyzeWhenDocumentIdsAreEmpty() {
        assertAnalyzeCountInvalid(new DocumentAnalyzeRequest(List.of()));
    }

    @Test
    void rejectsAnalyzeWhenDocumentCountExceedsLimit() {
        List<Long> documentIds = LongStream.rangeClosed(1L, 11L)
                .boxed()
                .toList();

        assertAnalyzeCountInvalid(new DocumentAnalyzeRequest(documentIds));
    }

    @Test
    void rejectsAnalyzeWhenDocumentDoesNotExist() {
        User user = user(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(documentRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> geminiService.analyzeDocuments(
                1L,
                new DocumentAnalyzeRequest(List.of(10L))
        ))
                .isInstanceOfSatisfying(GeneralException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(ErrorCode.DOCUMENT_NOT_FOUND));
    }

    @Test
    void rejectsAnalyzeWhenDocumentBelongsToDifferentUser() {
        User requester = user(1L);
        User owner = user(2L);
        Document document = document(owner, 10L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(documentRepository.findById(10L)).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> geminiService.analyzeDocuments(
                1L,
                new DocumentAnalyzeRequest(List.of(10L))
        ))
                .isInstanceOfSatisfying(GeneralException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(ErrorCode.DOCUMENT_ACCESS_DENIED));
    }

    private void assertAnalyzeCountInvalid(DocumentAnalyzeRequest request) {
        assertThatThrownBy(() -> geminiService.analyzeDocuments(1L, request))
                .isInstanceOfSatisfying(GeneralException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(ErrorCode.DOCUMENT_ANALYZE_COUNT_INVALID));
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

    private Document document(User user, Long id) {
        Document document = Document.builder()
                .user(user)
                .originalFileName("paystub.pdf")
                .storedFileName("uuid_paystub.pdf")
                .objectKey("documents/" + user.getId() + "/PAYSLIP/2026/04/uuid_paystub.pdf")
                .contentType("application/pdf")
                .documentType(DocumentType.PAYSLIP)
                .status(DocumentStatus.UPLOADED)
                .build();
        ReflectionTestUtils.setField(document, "id", id);
        return document;
    }
}
