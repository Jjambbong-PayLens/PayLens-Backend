package com.Jjambbong.PayLens.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.Jjambbong.PayLens.document.domain.Analysis;
import com.Jjambbong.PayLens.document.domain.AnalysisStatus;
import com.Jjambbong.PayLens.document.domain.Document;
import com.Jjambbong.PayLens.document.domain.DocumentStatus;
import com.Jjambbong.PayLens.document.domain.DocumentType;
import com.Jjambbong.PayLens.document.dto.request.DocumentAnalyzeRequest;
import com.Jjambbong.PayLens.document.dto.request.GeminiReviewSubmitRequest;
import com.Jjambbong.PayLens.document.dto.response.GeminiCrossCheckResponse;
import com.Jjambbong.PayLens.document.dto.response.GeminiReviewResponse;
import com.Jjambbong.PayLens.document.repository.AnalysisRepository;
import com.Jjambbong.PayLens.document.repository.DocumentRepository;
import com.Jjambbong.PayLens.global.api.ErrorCode;
import com.Jjambbong.PayLens.global.config.AmazonConfig;
import com.Jjambbong.PayLens.global.exception.GeneralException;
import com.Jjambbong.PayLens.survey.repository.SurveyRepository;
import com.Jjambbong.PayLens.user.domain.User;
import com.Jjambbong.PayLens.user.domain.UserRole;
import com.Jjambbong.PayLens.user.domain.UserStatus;
import com.Jjambbong.PayLens.user.repository.UserRepository;
import com.google.genai.types.Part;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@ExtendWith(MockitoExtension.class)
class GeminiPipelineServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private AnalysisRepository analysisRepository;

    @Mock
    private SurveyRepository surveyRepository;

    @Mock
    private AnalyzeService analyzeService;

    @Mock
    private AmazonConfig amazonConfig;

    @Mock
    private S3Presigner s3Presigner;

    private TestGeminiPipelineService geminiPipelineService;

    @BeforeEach
    void setUp() {
        geminiPipelineService = new TestGeminiPipelineService(
                userRepository,
                documentRepository,
                analysisRepository,
                surveyRepository,
                analyzeService,
                amazonConfig,
                s3Presigner
        );
    }

    @Test
    void rejectsCrossCheckWhenOcrIsNotCompleted() {
        User user = user(1L);
        Document document = uploadedDocument(user, 10L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(documentRepository.findById(10L)).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> geminiPipelineService.crossCheck(1L, new DocumentAnalyzeRequest(List.of(10L))))
                .isInstanceOfSatisfying(GeneralException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(ErrorCode.DOCUMENT_ANALYZE_OCR_REQUIRED));
    }

    @Test
    void storesUserReviewRequiredWhenGeminiRequestsRecapture() {
        User user = user(1L);
        Document document = ocrCompletedDocument(user, 10L, "contract.jpg", "image/jpeg");
        geminiPipelineService.addResponse(recaptureRequiredResponse());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(documentRepository.findById(10L)).thenReturn(Optional.of(document));
        when(analyzeService.getOcrJson(document)).thenReturn("{\"file_name\":\"contract.jpg\",\"pages\":[]}");
        when(analyzeService.getDocumentAsBase64(document)).thenReturn("base64-image");
        when(analysisRepository.save(any(Analysis.class))).thenAnswer(invocation -> {
            Analysis analysis = invocation.getArgument(0);
            ReflectionTestUtils.setField(analysis, "id", 100L);
            return analysis;
        });

        GeminiCrossCheckResponse response = geminiPipelineService.crossCheck(1L, new DocumentAnalyzeRequest(List.of(10L)));

        assertThat(response.getStatus()).isEqualTo(AnalysisStatus.USER_REVIEW_REQUIRED);
        assertThat(response.isRecaptureRequired()).isTrue();
        assertThat(response.isUserReviewRequired()).isTrue();
        assertThat(geminiPipelineService.calls).hasSize(1);
    }

    @Test
    void storesUserReviewRequiredWhenCrossCheckFails() {
        User user = user(1L);
        Document document = ocrCompletedDocument(user, 10L, "payslip.pdf", "application/pdf");
        geminiPipelineService.addResponse(userReviewRequiredResponse());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(documentRepository.findById(10L)).thenReturn(Optional.of(document));
        when(analyzeService.getOcrJson(document)).thenReturn("{\"file_name\":\"payslip.pdf\",\"pages\":[]}");
        when(analyzeService.getDocumentAsBase64(document)).thenReturn("base64-pdf");
        when(analysisRepository.save(any(Analysis.class))).thenAnswer(invocation -> {
            Analysis analysis = invocation.getArgument(0);
            ReflectionTestUtils.setField(analysis, "id", 101L);
            return analysis;
        });

        GeminiCrossCheckResponse response = geminiPipelineService.crossCheck(1L, new DocumentAnalyzeRequest(List.of(10L)));

        assertThat(response.getStatus()).isEqualTo(AnalysisStatus.USER_REVIEW_REQUIRED);
        assertThat(response.isUserReviewRequired()).isTrue();
        assertThat(response.isAutoAnalysisAvailable()).isFalse();
        assertThat(geminiPipelineService.calls).hasSize(1);
    }

    @Test
    void storesReadyForAnalysisWhenAllCrossChecksPass() {
        User user = user(1L);
        Document document = ocrCompletedDocument(user, 10L, "payslip.pdf", "application/pdf");
        geminiPipelineService.addResponse(autoAnalysisAvailableResponse());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(documentRepository.findById(10L)).thenReturn(Optional.of(document));
        when(analyzeService.getOcrJson(document)).thenReturn("{\"file_name\":\"payslip.pdf\",\"pages\":[]}");
        when(analyzeService.getDocumentAsBase64(document)).thenReturn("base64-pdf");
        when(analysisRepository.save(any(Analysis.class))).thenAnswer(invocation -> {
            Analysis analysis = invocation.getArgument(0);
            ReflectionTestUtils.setField(analysis, "id", 102L);
            return analysis;
        });

        GeminiCrossCheckResponse response = geminiPipelineService.crossCheck(1L, new DocumentAnalyzeRequest(List.of(10L)));

        assertThat(response.getStatus()).isEqualTo(AnalysisStatus.READY_FOR_ANALYSIS);
        assertThat(response.isAutoAnalysisAvailable()).isTrue();
        assertThat(geminiPipelineService.calls).hasSize(1);
    }

    @Test
    void reviewResponseContainsPresignedViewUrl() throws MalformedURLException {
        User user = user(1L);
        Document document = ocrCompletedDocument(user, 10L, "contract.jpg", "image/jpeg");
        Analysis analysis = Analysis.builder()
                .user(user)
                .fieldExtractionJson(userReviewRequiredResponse())
                .status(AnalysisStatus.USER_REVIEW_REQUIRED)
                .build();
        ReflectionTestUtils.setField(analysis, "id", 200L);
        analysis.addDocument(document);
        PresignedGetObjectRequest getRequest = mock(PresignedGetObjectRequest.class);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(analysisRepository.findById(200L)).thenReturn(Optional.of(analysis));
        when(amazonConfig.getBucket()).thenReturn("paylens-dev-documents");
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(getRequest);
        when(getRequest.url()).thenReturn(URI.create("https://example.com/contract").toURL());

        GeminiReviewResponse response = geminiPipelineService.getReview(1L, 200L);

        assertThat(response.getStatus()).isEqualTo(AnalysisStatus.USER_REVIEW_REQUIRED);
        assertThat(response.getDocumentGroups().toString()).contains("https://example.com/contract");
        verify(s3Presigner).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    void submitReviewStoresReviewedFields() {
        User user = user(1L);
        Analysis analysis = Analysis.builder()
                .user(user)
                .fieldExtractionJson(userReviewRequiredResponse())
                .status(AnalysisStatus.USER_REVIEW_REQUIRED)
                .build();
        ReflectionTestUtils.setField(analysis, "id", 300L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(analysisRepository.findById(300L)).thenReturn(Optional.of(analysis));

        GeminiReviewSubmitRequest request = new GeminiReviewSubmitRequest(List.of(
                new GeminiReviewSubmitRequest.DocumentGroupReviewRequest("GROUP_1", Map.<String, Object>of("총지급액", "2,300,000원"))
        ));

        geminiPipelineService.submitReview(1L, 300L, request);

        assertThat(analysis.getStatus()).isEqualTo(AnalysisStatus.REVIEW_COMPLETED);
        assertThat(analysis.getReviewedFieldsJson()).contains("2,300,000원");
    }

    @Test
    void analyzeRunsFinalAnalysisWhenReadyForAnalysis() {
        User user = user(1L);
        Document document = ocrCompletedDocument(user, 10L, "payslip.pdf", "application/pdf");
        Analysis analysis = Analysis.builder()
                .user(user)
                .fieldExtractionJson(autoAnalysisAvailableResponse())
                .status(AnalysisStatus.READY_FOR_ANALYSIS)
                .build();
        ReflectionTestUtils.setField(analysis, "id", 400L);
        analysis.addDocument(document);
        geminiPipelineService.addResponse(finalAnalysisResponse());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(analysisRepository.findById(400L)).thenReturn(Optional.of(analysis));
        when(surveyRepository.findByUser(user)).thenReturn(Optional.empty());
        when(analyzeService.getDocumentAsBase64(document)).thenReturn("base64-pdf");

        geminiPipelineService.analyze(1L, 400L);

        assertThat(analysis.getStatus()).isEqualTo(AnalysisStatus.COMPLETED);
        assertThat(analysis.getResultJson()).contains("최종판단");
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

    private Document uploadedDocument(User user, Long id) {
        Document document = Document.builder()
                .user(user)
                .originalFileName("document.pdf")
                .storedFileName("uuid_document.pdf")
                .objectKey("documents/1/OTHER/2026/05/uuid_document.pdf")
                .contentType("application/pdf")
                .documentType(DocumentType.OTHER)
                .status(DocumentStatus.UPLOADED)
                .build();
        ReflectionTestUtils.setField(document, "id", id);
        return document;
    }

    private Document ocrCompletedDocument(User user, Long id, String fileName, String contentType) {
        Document document = Document.builder()
                .user(user)
                .originalFileName(fileName)
                .storedFileName("uuid_" + fileName)
                .objectKey("documents/1/OTHER/2026/05/uuid_" + fileName)
                .contentType(contentType)
                .documentType(DocumentType.OTHER)
                .status(DocumentStatus.UPLOADED)
                .build();
        ReflectionTestUtils.setField(document, "id", id);
        document.completeOcr("documents_ocr/1/OTHER/2026/05/" + fileName + "_ocr.json", 100L, null);
        return document;
    }

    private String recaptureRequiredResponse() {
        return """
                {"analysisUnit":{"totalGroups":1,"autoAnalysisAvailable":false,"userReviewRequired":true,"recaptureRequired":true},"documentGroups":[{"groupId":"GROUP_1","documentType":"EMPLOYMENT_CONTRACT","documentTypeLabel":"근로계약서","sourceDocuments":[{"documentId":10,"fileName":"contract.jpg","contentType":"image/jpeg","pages":[1]}],"crossCheckPassed":false,"userReviewRequired":true,"recaptureRequired":true,"reason":"문서가 흐림","missingFields":["기본급"],"extractedFields":{}}]}
                """;
    }

    private String userReviewRequiredResponse() {
        return """
                {"analysisUnit":{"totalGroups":1,"autoAnalysisAvailable":false,"userReviewRequired":true,"recaptureRequired":false},"documentGroups":[{"groupId":"GROUP_1","documentType":"PAYSLIP","documentTypeLabel":"급여명세서","sourceDocuments":[{"documentId":10,"fileName":"payslip.pdf","contentType":"application/pdf","pages":[1,2]}],"crossCheckPassed":false,"userReviewRequired":true,"recaptureRequired":false,"reason":"야간근로수당 확인 필요","missingFields":["야간근로수당"],"extractedFields":{"총지급액":"2,300,000원"}}]}
                """;
    }

    private String autoAnalysisAvailableResponse() {
        return """
                {"analysisUnit":{"totalGroups":1,"autoAnalysisAvailable":true,"userReviewRequired":false,"recaptureRequired":false},"documentGroups":[{"groupId":"GROUP_1","documentType":"PAYSLIP","documentTypeLabel":"급여명세서","sourceDocuments":[{"documentId":10,"fileName":"payslip.pdf","contentType":"application/pdf","pages":[1]}],"crossCheckPassed":true,"userReviewRequired":false,"recaptureRequired":false,"reason":null,"missingFields":[],"extractedFields":{"총지급액":"2,300,000원"}}]}
                """;
    }

    private String finalAnalysisResponse() {
        return """
                {"문서검증":{"불량문서여부":false},"최종판단":{"임금체불가능성":"낮음"}}
                """;
    }

    private static class TestGeminiPipelineService extends GeminiPipelineService {

        private final Queue<String> responses = new ArrayDeque<>();
        private final List<List<Part>> calls = new ArrayList<>();

        TestGeminiPipelineService(UserRepository userRepository,
                                  DocumentRepository documentRepository,
                                  AnalysisRepository analysisRepository,
                                  SurveyRepository surveyRepository,
                                  AnalyzeService analyzeService,
                                  AmazonConfig amazonConfig,
                                  S3Presigner s3Presigner) {
            super(userRepository, documentRepository, analysisRepository, surveyRepository,
                    analyzeService, amazonConfig, s3Presigner);
        }

        void addResponse(String response) {
            responses.add(response);
        }

        @Override
        protected String generateGeminiResponse(List<Part> parts) {
            calls.add(parts);
            return responses.remove();
        }
    }
}
