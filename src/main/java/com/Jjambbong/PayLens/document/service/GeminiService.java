package com.Jjambbong.PayLens.document.service;

import com.Jjambbong.PayLens.document.domain.Analysis;
import com.Jjambbong.PayLens.document.domain.Document;
import com.Jjambbong.PayLens.document.dto.request.DocumentAnalyzeRequest;
import com.Jjambbong.PayLens.document.repository.AnalysisRepository;
import com.Jjambbong.PayLens.document.repository.DocumentRepository;
import com.Jjambbong.PayLens.global.api.ErrorCode;
import com.Jjambbong.PayLens.global.exception.GeneralException;
import com.Jjambbong.PayLens.survey.domain.Survey;
import com.Jjambbong.PayLens.survey.repository.SurveyRepository;
import com.Jjambbong.PayLens.user.domain.User;
import com.Jjambbong.PayLens.user.repository.UserRepository;

import com.google.genai.Client;
import com.google.genai.types.Blob;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final SurveyRepository surveyRepository;
    private final AnalysisRepository analysisRepository;
    private final AnalyzeService analyzeService;

    @Deprecated
    public String analyzeDocument(Long userId, Long documentId) {
        return analyzeDocuments(userId, new DocumentAnalyzeRequest(List.of(documentId)));
    }

    public String analyzeDocuments(Long userId, DocumentAnalyzeRequest request) {

        List<Long> documentIds = validateAnalyzeDocumentIds(request);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        List<Document> documents = documentIds.stream()
                .map(documentId -> documentRepository.findById(documentId)
                        .orElseThrow(() -> new GeneralException(ErrorCode.DOCUMENT_NOT_FOUND)))
                .toList();

        documents.forEach(document -> {
            if (!document.getUser().getId().equals(user.getId())) {
                throw new GeneralException(ErrorCode.DOCUMENT_ACCESS_DENIED);
            }
        });

        String targetLanguage = user.getPreferredLanguage() != null
                ? user.getPreferredLanguage().getDescription()
                : "한국어";

        String surveyPrompt = buildSurveyPrompt(user);

        String prompt = String.format(
                """
                첨부 문서를 분석하여 임금체불 가능성을 JSON 형식으로 추출해줘.
                모든 키와 값의 언어는 "%s"로 작성해줘.
                JSON 외의 설명은 하지 마.

                먼저 첨부 문서가 임금체불 분석에 적합한 문서인지 검증해줘.

                이 사용자의 근무 환경은 다음과 같아:
                %s

                위 근무 환경 정보를 바탕으로 연장/야간/휴일근로수당(1.5배), 주휴수당, 퇴직금, 휴업수당 발생 여부를 반드시 반영해서 분석해줘.

                유효한 문서 유형:
                - 급여명세서
                - 근로계약서
                - 입금내역
                - 문자/카카오톡 캡처
                - 근무표
                - 출퇴근 기록
                - 퇴사 관련 자료
                - 공제내역서
                - 기타 임금, 근로시간, 급여 지급 여부를 확인할 수 있는 문서

                불량문서 판단 기준:
                - 임금, 급여, 근로시간, 입금액, 수당, 퇴직금, 근로계약, 출퇴근 기록과 관련 없는 문서이면 불량문서여부를 true로 작성해.
                - 강의자료, 과제자료, 일반 문서, 이미지가 깨진 파일, 읽을 수 없는 PDF, 내용이 거의 없는 문서는 불량문서여부를 true로 작성해.
                - 불량문서여부가 true이면 임금체불분석가능여부는 false로 작성해.
                - 문서적합도는 "적합", "부분적합", "부적합" 중 하나로 작성해.
                - 문서가 부적합하면 분석 항목은 무리하게 추측하지 말고 null 또는 "판단불가"로 작성해.

                분석 항목:
                1. 최저임금 및 주휴수당
                2. 퇴직금 미지급
                3. 야간근로수당 및 휴업수당
                4. 초과근로수당, 휴일근로수당, 연차수당
                5. 필요한 증거와 부족한 자료

                판단 기준:
                - 최저시급은 10,320원으로 판단해.
                - 주 15시간 이상 근로 여부를 확인해.
                - 5인 이상 사업장 여부를 확인해.
                - 근로시간, 급여액, 입금액, 수당 지급 여부를 확인해.
                - 확인할 수 없는 항목은 null로 작성해.
                - 근거가 부족하면 "판단불가" 또는 "추가자료필요"로 작성해.
                - 임금체불이라고 단정하지 말고 가능성 중심으로 분석해.

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
                """,
                targetLanguage,
                surveyPrompt
        );

        try {
            Client client = Client.builder()
                    .apiKey(geminiApiKey)
                    .build();

            Part textPart = Part.builder().text(prompt).build();

            List<Part> parts = new ArrayList<>();
            parts.add(textPart);

            documents.stream()
                    .map(analyzeService::getDocumentAsBase64)
                    .map(base64Pdf -> Part.builder()
                            .inlineData(Blob.builder()
                                    .mimeType("application/pdf")
                                    .data(base64Pdf)
                                    .build())
                            .build())
                    .forEach(parts::add);

            Content content = Content.builder()
                    .parts(parts)
                    .build();

            GenerateContentResponse response = client.models.generateContent(
                    "gemini-3-flash-preview",
                    content,
                    null
            );
            // 제미나이로부터 받은 JSON 응답 문자열
            String analysisResultJson = response.text();

            // 새로운 분석(Analysis) 객체를 생성
            Analysis analysis = Analysis.builder()
                    .user(user)
                    .resultJson(analysisResultJson)
                    .build();

            // 이 분석에 사용된 모든 문서(Document)들을 순회하며 관계를 맺어줌
            for (Document doc : documents) {
                doc.setAnalysis(analysis);
            }

            analysisRepository.save(analysis);

            return analysisResultJson;

        } catch (Exception e) {
            log.error("Gemini SDK 호출 중 에러 발생: {}", e.getMessage());
            throw new GeneralException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private List<Long> validateAnalyzeDocumentIds(DocumentAnalyzeRequest request) {
        if (request == null || request.getDocumentIds() == null
                || request.getDocumentIds().isEmpty() || request.getDocumentIds().size() > 10) {
            throw new GeneralException(ErrorCode.DOCUMENT_ANALYZE_COUNT_INVALID);
        }

        return request.getDocumentIds();
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
        sb.append(survey.isHasUnscheduledDayOff() ? "- 사용자의 귀책사유 없이 휴업한 날(갑자기 쉬라고 한 날)이 존재합니다. (휴업수당 70% 발생 가능성 검토 요망)\n" : "- 휴업한 날이 없습니다.\n");

        return sb.toString();
    }
}
