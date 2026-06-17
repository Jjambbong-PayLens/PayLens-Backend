package com.Jjambbong.PayLens.document.service;

import com.Jjambbong.PayLens.document.domain.Analysis;
import com.Jjambbong.PayLens.document.domain.AnalysisStatus;
import com.Jjambbong.PayLens.document.domain.Document;
import com.Jjambbong.PayLens.document.domain.DocumentOcrStatus;
import com.Jjambbong.PayLens.document.domain.DocumentStatus;
import com.Jjambbong.PayLens.document.dto.request.DocumentAnalyzeRequest;
import com.Jjambbong.PayLens.document.dto.request.GeminiReviewSubmitRequest;
import com.Jjambbong.PayLens.document.dto.response.GeminiAnalyzeResponse;
import com.Jjambbong.PayLens.document.dto.response.GeminiCrossCheckResponse;
import com.Jjambbong.PayLens.document.dto.response.GeminiReviewResponse;
import com.Jjambbong.PayLens.document.dto.response.GeminiReviewSubmitResponse;
import com.Jjambbong.PayLens.document.repository.AnalysisRepository;
import com.Jjambbong.PayLens.document.repository.DocumentRepository;
import com.Jjambbong.PayLens.global.api.ErrorCode;
import com.Jjambbong.PayLens.global.config.AmazonConfig;
import com.Jjambbong.PayLens.global.exception.GeneralException;
import com.Jjambbong.PayLens.survey.domain.Survey;
import com.Jjambbong.PayLens.survey.repository.SurveyRepository;
import com.Jjambbong.PayLens.user.domain.User;
import com.Jjambbong.PayLens.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.genai.Client;
import com.google.genai.types.Blob;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GeminiPipelineService {

    private static final int MAX_ANALYZE_DOCUMENT_COUNT = 10;
    private static final long REVIEW_URL_EXPIRATION_SECONDS = 600L;
    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final AnalysisRepository analysisRepository;
    private final SurveyRepository surveyRepository;
    private final AnalyzeService analyzeService;
    private final AmazonConfig amazonConfig;
    private final S3Presigner s3Presigner;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiCrossCheckResponse crossCheck(Long userId, DocumentAnalyzeRequest request) {
        User user = getUser(userId);
        List<Document> documents = findValidatedDocuments(user, request);

        String extractionJson = extractFieldsWithGemini(user, documents);
        CrossCheckDecision decision = decide(extractionJson);

        Analysis analysis = Analysis.builder()
                .user(user)
                .fieldExtractionJson(extractionJson)
                .status(decision.status())
                .build();
        documents.forEach(analysis::addDocument);
        analysisRepository.save(analysis);

        return GeminiCrossCheckResponse.of(
                analysis,
                decision.autoAnalysisAvailable(),
                decision.userReviewRequired(),
                decision.recaptureRequired()
        );
    }

    @Transactional(readOnly = true)
    public GeminiReviewResponse getReview(Long userId, Long analysisId) {
        User user = getUser(userId);
        Analysis analysis = findOwnedAnalysis(user, analysisId);

        if (analysis.getStatus() != AnalysisStatus.USER_REVIEW_REQUIRED) {
            throw new GeneralException(ErrorCode.GEMINI_REVIEW_STATUS_INVALID);
        }

        return GeminiReviewResponse.builder()
                .analysisId(analysis.getId())
                .status(analysis.getStatus())
                .documentGroups(buildReviewDocumentGroups(analysis))
                .build();
    }

    public GeminiReviewSubmitResponse submitReview(Long userId, Long analysisId, GeminiReviewSubmitRequest request) {
        User user = getUser(userId);
        Analysis analysis = findOwnedAnalysis(user, analysisId);

        if (analysis.getStatus() != AnalysisStatus.USER_REVIEW_REQUIRED) {
            throw new GeneralException(ErrorCode.GEMINI_REVIEW_STATUS_INVALID);
        }
        if (request == null || request.getDocumentGroups() == null || request.getDocumentGroups().isEmpty()) {
            throw new GeneralException(ErrorCode.BAD_REQUEST);
        }

        try {
            String reviewedFieldsJson = objectMapper.writeValueAsString(request);
            analysis.completeReview(reviewedFieldsJson);
            return GeminiReviewSubmitResponse.from(analysis);
        } catch (JsonProcessingException e) {
            throw new GeneralException(ErrorCode.GEMINI_RESPONSE_INVALID);
        }
    }

    public GeminiAnalyzeResponse analyze(Long userId, Long analysisId) {
        User user = getUser(userId);
        Analysis analysis = findOwnedAnalysis(user, analysisId);

        if (analysis.getStatus() != AnalysisStatus.READY_FOR_ANALYSIS
                && analysis.getStatus() != AnalysisStatus.REVIEW_COMPLETED) {
            throw new GeneralException(ErrorCode.GEMINI_ANALYZE_STATUS_INVALID);
        }

        String verifiedFieldsJson = analysis.getStatus() == AnalysisStatus.REVIEW_COMPLETED
                ? analysis.getReviewedFieldsJson()
                : analysis.getFieldExtractionJson();

        if (verifiedFieldsJson == null || verifiedFieldsJson.isBlank()) {
            throw new GeneralException(ErrorCode.GEMINI_RESPONSE_INVALID);
        }

        String resultJson = analyzeVerifiedFields(user, verifiedFieldsJson, analysis.getDocuments());
        analysis.completeWithResult(resultJson);
        return GeminiAnalyzeResponse.of(analysis, objectMapper);
    }

    private String extractFieldsWithGemini(User user, List<Document> documents) {
        List<Part> parts = new ArrayList<>();
        parts.add(Part.builder().text(buildCrossCheckPrompt()).build());

        for (Document document : documents) {
            parts.add(Part.builder().text(buildDocumentContext(document, analyzeService.getOcrJson(document))).build());
            parts.add(Part.builder()
                    .inlineData(Blob.builder()
                            .mimeType(document.getContentType())
                            .data(analyzeService.getDocumentAsBase64(document))
                            .build())
                    .build());
        }

        return parseGeminiJson(generateGeminiResponse(parts));
    }

    private String analyzeVerifiedFields(User user, String verifiedFieldsJson, List<Document> documents) {
        String targetLanguage = user.getPreferredLanguage() != null
                ? user.getPreferredLanguage().getDescription()
                : "한국어";

        String prompt = buildFinalAnalysisPrompt(targetLanguage, buildSurveyPrompt(user), verifiedFieldsJson);
        List<Part> parts = new ArrayList<>();
        parts.add(Part.builder().text(prompt).build());

        for (Document document : documents) {
            parts.add(Part.builder().text(buildOriginalDocumentContext(document)).build());
            parts.add(Part.builder()
                    .inlineData(Blob.builder()
                            .mimeType(document.getContentType())
                            .data(analyzeService.getDocumentAsBase64(document))
                            .build())
                    .build());
        }

        return parseGeminiJson(generateGeminiResponse(parts));
    }

    protected String generateGeminiResponse(List<Part> parts) {
        try {
            Client client = Client.builder()
                    .apiKey(geminiApiKey)
                    .build();

            Content content = Content.builder()
                    .parts(parts)
                    .build();

            GenerateContentResponse response = client.models.generateContent(
                    "gemini-3.5-flash",
                    content,
                    null
            );
            return response.text();
        } catch (Exception e) {
            log.error("Gemini 파이프라인 호출 중 에러 발생: {}", e.getMessage());
            throw new GeneralException(ErrorCode.GEMINI_FIELD_EXTRACTION_FAILED);
        }
    }

    private List<Document> findValidatedDocuments(User user, DocumentAnalyzeRequest request) {
        List<Long> documentIds = validateDocumentIds(request);
        return documentIds.stream()
                .map(documentId -> findValidatedDocument(user, documentId))
                .toList();
    }

    private List<Long> validateDocumentIds(DocumentAnalyzeRequest request) {
        if (request == null || request.getDocumentIds() == null
                || request.getDocumentIds().isEmpty()
                || request.getDocumentIds().size() > MAX_ANALYZE_DOCUMENT_COUNT
                || request.getDocumentIds().stream().anyMatch(documentId -> documentId == null)
                || request.getDocumentIds().stream().distinct().count() != request.getDocumentIds().size()) {
            throw new GeneralException(ErrorCode.DOCUMENT_ANALYZE_COUNT_INVALID);
        }
        return request.getDocumentIds();
    }

    private Document findValidatedDocument(User user, Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new GeneralException(ErrorCode.DOCUMENT_NOT_FOUND));

        if (!document.getUser().getId().equals(user.getId())) {
            throw new GeneralException(ErrorCode.DOCUMENT_ACCESS_DENIED);
        }
        if (document.getStatus() != DocumentStatus.UPLOADED
                || document.getOcrStatus() != DocumentOcrStatus.COMPLETED
                || document.getOcrResultKey() == null
                || document.getOcrResultKey().isBlank()) {
            throw new GeneralException(ErrorCode.DOCUMENT_ANALYZE_OCR_REQUIRED);
        }
        return document;
    }

    private Analysis findOwnedAnalysis(User user, Long analysisId) {
        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new GeneralException(ErrorCode.ANALYSIS_NOT_FOUND));

        if (!analysis.getUser().getId().equals(user.getId())) {
            throw new GeneralException(ErrorCode.DOCUMENT_ACCESS_DENIED);
        }
        return analysis;
    }

    private User getUser(Long userId) {
        if (userId == null) {
            throw new GeneralException(ErrorCode.TOKEN_INVALID);
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));
    }

    private CrossCheckDecision decide(String extractionJson) {
        try {
            JsonNode root = objectMapper.readTree(extractionJson);
            JsonNode groups = root.path("documentGroups");
            boolean recaptureRequired = root.path("analysisUnit").path("recaptureRequired").asBoolean(false);
            boolean userReviewRequired = root.path("analysisUnit").path("userReviewRequired").asBoolean(false);
            boolean hasGroup = groups.isArray() && groups.size() > 0;
            boolean allCrossCheckPassed = hasGroup;

            if (groups.isArray()) {
                for (JsonNode group : groups) {
                    recaptureRequired = recaptureRequired || group.path("recaptureRequired").asBoolean(false);
                    boolean crossCheckPassed = group.path("crossCheckPassed").asBoolean(false);
                    allCrossCheckPassed = allCrossCheckPassed && crossCheckPassed;
                    userReviewRequired = userReviewRequired || !crossCheckPassed || group.path("userReviewRequired").asBoolean(false);
                }
            }

            if (recaptureRequired || !allCrossCheckPassed || userReviewRequired) {
                return new CrossCheckDecision(AnalysisStatus.USER_REVIEW_REQUIRED, false, true, recaptureRequired);
            }
            return new CrossCheckDecision(AnalysisStatus.READY_FOR_ANALYSIS, true, false, false);
        } catch (JsonProcessingException e) {
            throw new GeneralException(ErrorCode.GEMINI_RESPONSE_INVALID);
        }
    }

    private Object buildReviewDocumentGroups(Analysis analysis) {
        try {
            JsonNode root = objectMapper.readTree(analysis.getFieldExtractionJson());
            JsonNode groups = root.path("documentGroups");
            if (!groups.isArray()) {
                throw new GeneralException(ErrorCode.GEMINI_RESPONSE_INVALID);
            }

            for (JsonNode group : groups) {
                JsonNode sourceDocuments = group.path("sourceDocuments");
                if (!sourceDocuments.isArray()) {
                    continue;
                }
                for (JsonNode sourceDocument : sourceDocuments) {
                    if (!sourceDocument.isObject()) {
                        continue;
                    }
                    Long documentId = sourceDocument.path("documentId").isNumber()
                            ? sourceDocument.path("documentId").asLong()
                            : null;
                    if (documentId == null) {
                        continue;
                    }
                    Document document = analysis.getDocuments().stream()
                            .filter(candidate -> candidate.getId().equals(documentId))
                            .findFirst()
                            .orElseThrow(() -> new GeneralException(ErrorCode.DOCUMENT_NOT_FOUND));
                    ((ObjectNode) sourceDocument).put("viewUrl", createPresignedGetUrl(document.getObjectKey()));
                }
            }
            return objectMapper.convertValue(groups, Object.class);
        } catch (JsonProcessingException e) {
            throw new GeneralException(ErrorCode.GEMINI_RESPONSE_INVALID);
        }
    }

    private String createPresignedGetUrl(String objectKey) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(amazonConfig.getBucket())
                    .key(objectKey)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(REVIEW_URL_EXPIRATION_SECONDS))
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (RuntimeException e) {
            throw new GeneralException(ErrorCode.PRESIGNED_URL_GENERATION_FAILED);
        }
    }

    private String parseGeminiJson(String responseText) {
        if (responseText == null || responseText.isBlank()) {
            throw new GeneralException(ErrorCode.GEMINI_RESPONSE_INVALID);
        }
        String json = stripJsonFence(responseText.trim());
        try {
            objectMapper.readTree(json);
            return json;
        } catch (JsonProcessingException e) {
            throw new GeneralException(ErrorCode.GEMINI_RESPONSE_INVALID);
        }
    }

    private String stripJsonFence(String text) {
        if (text.startsWith("```")) {
            int firstLineEnd = text.indexOf('\n');
            int lastFence = text.lastIndexOf("```");
            if (firstLineEnd >= 0 && lastFence > firstLineEnd) {
                return text.substring(firstLineEnd + 1, lastFence).trim();
            }
        }
        return text;
    }


    private String buildOriginalDocumentContext(Document document) {
        return """
                최종 분석 참고용 원본 문서 메타데이터이다.
                documentId: %d
                fileName: %s
                contentType: %s
                """.formatted(
                document.getId(),
                document.getOriginalFileName(),
                document.getContentType()
        );
    }

    private String buildDocumentContext(Document document, String ocrJson) {
        return """
                다음 문서의 메타데이터와 PaddleOCR-VL-1.5 OCR JSON이다.
                documentId: %d
                fileName: %s
                contentType: %s
                originalUploadType: %s
                OCR JSON:
                %s
                """.formatted(
                document.getId(),
                document.getOriginalFileName(),
                document.getContentType(),
                document.getDocumentType().name(),
                ocrJson
        );
    }

    private String buildCrossCheckPrompt() {
        return """
                첨부된 여러 원본 문서와 각 문서의 OCR JSON을 함께 분석해줘.
                입력에는 여러 페이지 PDF와 이미지 문서가 섞여 있을 수 있다.

                목표:
                1. 각 문서 또는 페이지를 급여명세서, 근로계약서, 기타 문서로 분류한다.
                2. PDF/이미지 형식과 관계없이, 내용상 같은 문서에 속하는 파일이나 페이지는 하나의 documentGroup으로 묶는다.
                3. OCR JSON의 PaddleOCR-VL-1.5 결과와 원본 문서를 기반으로 네가 다시 읽은 결과를 비교한다.
                4. 자동 분석은 보수적으로 판단한다. 핵심 필드가 명확히 일치하고 원본 문서가 밝고 선명하며 전체 영역이 잘 보일 때만 crossCheckPassed=true로 작성한다.
                5. 촬영 이미지라도 밝기, 초점, 해상도, 기울기, 잘림 문제가 없고 핵심 필드를 안정적으로 읽을 수 있으면 crossCheckPassed=true로 작성할 수 있다.
                6. 핵심 필드가 불일치/누락/불명확하거나 원본 문서가 흐림, 어두움, 저대비, 그림자, 노이즈, 큰 기울어짐, 가림, 잘림, 해상도 부족 상태이면 crossCheckPassed=false 및 userReviewRequired=true로 작성한다.
                7. 문서가 너무 흐리거나 잘려 핵심 정보를 읽을 수 없으면 recaptureRequired=true 및 userReviewRequired=true로 작성한다.
                8. 문서를 읽을 수는 있지만 품질이 낮은 경우에는 recaptureRequired=false, crossCheckPassed=false, userReviewRequired=true로 작성한다.
                9. 확실하지 않은 값은 추측하지 말고 null로 작성한다.
                10. JSON 외의 설명은 하지 않는다.

                문서 유형 분류 기준:
                - PAYSLIP: 기본급, 수당, 총지급액, 공제액, 실수령액, 임금지급일 등 실제 지급된 급여 내역
                - EMPLOYMENT_CONTRACT: 근로계약기간, 근로시간, 휴게시간, 휴일, 월 통상임금, 임금지급방법, 숙식 제공 조건 등 약정 근로조건
                - OTHER: 위 두 유형이 아니거나 임금 분석에 직접 사용하기 어려운 문서

                문서 그룹화 기준:
                - 파일 형식이 PDF인지 이미지인지로 그룹을 나누지 말고, 문서 내용 기준으로 그룹화한다.
                - 같은 근로자명, 같은 업체명, 같은 문서 유형, 같은 급여월 또는 같은 계약기간처럼 보이는 문서는 하나의 그룹으로 묶는다.
                - 서로 다른 급여월, 근로자, 업체의 급여명세서는 별도 PAYSLIP 그룹으로 분리한다.
                - 서로 다른 계약기간, 근로자, 업체의 근로계약서는 별도 EMPLOYMENT_CONTRACT 그룹으로 분리한다.

                문서 품질 판단 기준:
                - 자동 분석 가능은 원본이 충분히 밝고 선명하며, 핵심 금액/날짜/이름/근로조건을 확대 없이 안정적으로 확인할 수 있는 경우로 제한한다.
                - 촬영 이미지라는 이유만으로 사용자 검증 대상으로 보내지 않는다. 품질이 충분히 좋으면 자동 분석 가능하다.
                - 약한 흐림, 어두움, 저대비, 그림자, 노이즈, 기울어짐처럼 읽을 수는 있지만 오인식 가능성이 있는 문서는 재촬영 대신 사용자 검증 대상으로 보낸다.
                - 특히 어두운 배경, 초점 흐림, 압축 손상, 일부 항목 번짐, 표/금액 경계가 불명확한 경우에는 글자를 어느 정도 읽을 수 있어도 사용자 검증 대상으로 보낸다.

                recaptureRequired 판단 기준:
                - true: 심한 흐림, 잘림, 회전, 가림, 해상도 부족, 페이지 누락 등으로 핵심 정보를 읽기 어려움. 이 경우에도 userReviewRequired=true로 작성한다.
                - false: 문서 자체는 읽을 수 있음. 일부 필드만 불확실하거나 품질이 약간 낮으면 recaptureRequired는 false, crossCheckPassed는 false, userReviewRequired는 true로 작성함

                analysisUnit 작성 기준:
                - autoAnalysisAvailable은 모든 documentGroups의 crossCheckPassed가 true이고 recaptureRequired가 false이며 원본 문서 품질이 모두 양호할 때만 true로 작성한다.
                - userReviewRequired는 하나라도 userReviewRequired=true이거나 recaptureRequired=true인 그룹이 있으면 true로 작성한다.
                - recaptureRequired는 하나라도 recaptureRequired=true인 그룹이 있으면 true로 작성한다.

                reason에는 사용자 검증 또는 재촬영이 필요한 이유를 간단히 작성한다. 품질 저하로 사용자 검증이 필요하면 "문서가 흐리거나 어두워 사용자의 확인이 필요함"처럼 작성하고, 재촬영이 필요하면 "문서가 심하게 흐려 재촬영이 필요함"처럼 작성한다. 문제가 없으면 null로 작성한다.

                급여명세서 추출 필드:
                업체명, 근로자명, 기본급, 연장근로수당, 야간근로수당, 휴일근로수당, 가족수당/식대, 총지급액, 총공제액, 실수령액, 임금지급일

                근로계약서 추출 필드:
                업체명, 근로자명, 근로계약기간, 근로시간, 휴게시간, 휴일, 월 통상임금, 기본급, 고정수당, 상여금, 임금지급일, 임금지급방법, 숙식 제공, 근로자 부담금

                응답 형식:
                {
                  "analysisUnit": {
                    "totalGroups": null,
                    "autoAnalysisAvailable": null,
                    "userReviewRequired": null,
                    "recaptureRequired": null
                  },
                  "documentGroups": [
                    {
                      "groupId": "GROUP_1",
                      "documentType": "PAYSLIP | EMPLOYMENT_CONTRACT | OTHER",
                      "documentTypeLabel": "급여명세서 | 근로계약서 | 기타",
                      "sourceDocuments": [
                        {
                          "documentId": null,
                          "fileName": null,
                          "contentType": null,
                          "pages": []
                        }
                      ],
                      "crossCheckPassed": null,
                      "userReviewRequired": null,
                      "recaptureRequired": null,
                      "reason": null,
                      "missingFields": [],
                      "extractedFields": {}
                    }
                  ]
                }
                """;
    }

    private String buildFinalAnalysisPrompt(String targetLanguage, String surveyPrompt, String verifiedFieldsJson) {
        return """
                검증된 문서 필드 JSON과 함께 첨부된 원본 문서를 참고하여 임금체불 가능성을 JSON 형식으로 분석해줘.
                모든 키와 값의 언어는 "%s"로 작성하고, JSON 외의 설명은 하지 마.

                사용자 근무 환경:
                %s

                검증된 문서 필드 JSON:
                %s

                분석 전제:
                - 입력 JSON은 PaddleOCR-Gemini 교차 검증을 통과했거나 사용자가 확인/수정한 필드이다.
                - 첨부된 원본 문서는 검증된 필드의 근거 확인과 누락 맥락 보조용으로 사용한다.
                - 입력 JSON과 원본 문서가 충돌하면 사용자가 확인한 입력 JSON 값을 우선한다.
                - documentGroups에는 근로계약서, 급여명세서, 기타 문서가 포함될 수 있다.
                - 근로계약서만 있거나, 급여명세서만 있거나, 둘 다 있는 경우를 모두 고려해.

                분석 규칙:
                - 근로계약서와 급여명세서가 모두 있으면 약정 근로조건/임금과 실제 지급 내역을 비교해.
                - 근로계약서만 있으면 약정 조건 기준으로 위험 요소와 추가로 필요한 급여명세서/입금내역을 작성해.
                - 급여명세서만 있으면 실제 지급액, 수당, 공제액, 실수령액을 중심으로 분석하고 근로시간/계약 정보가 필요한 항목은 "판단불가" 또는 "추가자료필요"로 작성해.
                - 임금, 급여, 근로시간, 수당, 퇴직금, 근로계약과 관련된 정보가 없으면 불량문서여부를 true로 작성해.
                - 임금 분석에 사용할 핵심 정보가 거의 없으면 불량문서여부를 true, 임금체불분석가능여부를 false로 작성해.
                - 문서적합도는 "적합", "부분적합", "부적합" 중 하나로 작성해.
                - 문서가 부적합하면 분석 항목은 추측하지 말고 null 또는 "판단불가"로 작성해.
                - 최저시급은 10,320원으로 판단하고, 주 15시간 이상 여부, 5인 이상 사업장 여부, 연장/야간/휴일근로수당(1.5배), 주휴수당, 퇴직금, 휴업수당을 반영해.
                - 금액, 날짜, 시간은 입력 JSON 값을 우선 사용하고, 확인할 수 없는 값은 null로 작성해.
                - 근거가 부족하면 "판단불가" 또는 "추가자료필요"로 작성하고, 임금체불이라고 단정하지 말고 가능성 중심으로 분석해.

                응답 형식:
                {
                  "문서검증": {
                    "불량문서여부": null,
                    "불량문서사유": null,
                    "임금체불분석가능여부": null,
                    "문서적합도": null
                  },
                  "문서요약": {
                    "문서유형": null,
                    "근로자명": null,
                    "사업장명": null,
                    "확인된기간": null,
                    "분석요약": null
                  },
                  "공통추출항목": {
                    "시급": null,
                    "최저시급": 10320,
                    "주당근로시간": null,
                    "일일근로시간": null,
                    "재직시작일": null,
                    "재직종료일": null,
                    "퇴사일": null,
                    "급여지급일": null,
                    "기본급": null,
                    "총지급액": null,
                    "총공제액": null,
                    "실수령액": null,
                    "입금액": null,
                    "사업장근로자수_5인이상여부": null
                  },
                  "임금체불분석": {
                    "최저임금및주휴수당": {
                      "위반가능성": null,
                      "판단근거": [],
                      "부족한증거": []
                    },
                    "퇴직금미지급": {
                      "미지급가능성": null,
                      "예상퇴직금": null,
                      "판단근거": [],
                      "부족한증거": []
                    },
                    "야간근로수당및휴업수당": {
                      "미지급가능성": null,
                      "판단근거": [],
                      "부족한증거": []
                    },
                    "초과근로수당휴일근로수당연차수당": {
                      "미지급가능성": null,
                      "판단근거": [],
                      "부족한증거": []
                    }
                  },
                  "최종판단": {
                    "임금체불가능성": null,
                    "가장의심되는항목": [],
                    "추가로필요한자료": [],
                    "사용자에게보여줄설명": null,
                    "주의문구": "본 분석은 AI의 1차 검토 결과이며 법률적 판단이 아닙니다."
                  }
                }
                """.formatted(targetLanguage, surveyPrompt, verifiedFieldsJson);
    }


    private String buildSurveyPrompt(User user) {
        Optional<Survey> surveyOpt = surveyRepository.findByUser(user);
        if (surveyOpt.isEmpty()) {
            return "- 사용자가 아직 문진표를 작성하지 않았습니다. 일반적인 근로기준법을 바탕으로 분석해주세요.";
        }

        Survey survey = surveyOpt.get();
        StringBuilder sb = new StringBuilder();
        sb.append(survey.isOverFiveEmployees() ? "- 상시 근로자 5인 이상 사업장입니다. (연장/야간/휴일수당 1.5배 가산 적용 대상)\n" : "- 상시 근로자 5인 미만 사업장입니다. (가산수당 미적용)\n");
        sb.append(survey.isWorkingOverFifteenHours() ? "- 1주 소정근로시간이 15시간 이상입니다. (주휴수당 발생 대상)\n" : "- 1주 소정근로시간이 15시간 미만(초단시간 근로자)입니다. (주휴수당 미발생)\n");
        sb.append(survey.isWorkingOverOneYear() ? "- 계속근로기간이 1년 이상입니다. (퇴직금 발생 대상)\n" : "- 계속근로기간이 1년 미만입니다. (퇴직금 미발생)\n");
        sb.append(survey.isHasUnscheduledDayOff() ? "- 사용자의 귀책사유 없이 휴업한 날이 존재합니다. (휴업수당 70% 발생 가능성 검토 요망)\n" : "- 휴업한 날이 없습니다.\n");
        return sb.toString();
    }

    private record CrossCheckDecision(AnalysisStatus status, boolean autoAnalysisAvailable,
                                      boolean userReviewRequired, boolean recaptureRequired) { }
}
