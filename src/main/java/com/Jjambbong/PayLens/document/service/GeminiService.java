package com.Jjambbong.PayLens.document.service;

import com.Jjambbong.PayLens.document.domain.Document;
import com.Jjambbong.PayLens.document.repository.DocumentRepository;
import com.Jjambbong.PayLens.global.api.ErrorCode;
import com.Jjambbong.PayLens.global.exception.GeneralException;
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

import java.util.Arrays;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final AnalyzeService analyzeService;

    public String analyzeDocument(Long userId, Long documentId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new GeneralException(ErrorCode.DOCUMENT_NOT_FOUND));

        if (!document.getUser().getId().equals(user.getId())) {
            throw new GeneralException(ErrorCode.DOCUMENT_ACCESS_DENIED);
        }

        String base64Pdf = analyzeService.getDocumentAsBase64(document);
        byte[] pdfBytes = Base64.getDecoder().decode(base64Pdf);

        String targetLanguage = user.getPreferredLanguage() != null
                ? user.getPreferredLanguage().getDescription()
                : "한국어";

        String prompt = String.format(
                """
                첨부 문서를 분석하여 임금체불 가능성을 JSON 형식으로 추출해줘.
                모든 키와 값의 언어는 "%s"로 작성해줘.
                JSON 외의 설명은 하지 마.

                먼저 첨부 문서가 임금체불 분석에 적합한 문서인지 검증해줘.

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
                targetLanguage
        );

        try {
            Client client = Client.builder()
                    .apiKey(geminiApiKey)
                    .build();

            Part textPart = Part.builder().text(prompt).build();

            Part pdfPart = Part.builder()
                    .inlineData(Blob.builder()
                            .mimeType("application/pdf")
                            .data(base64Pdf)
                            .build())
                    .build();

            Content content = Content.builder()
                    .parts(Arrays.asList(textPart, pdfPart))
                    .build();

            GenerateContentResponse response = client.models.generateContent(
                    "gemini-3-flash-preview",
                    content,
                    null
            );

            return response.text();

        } catch (Exception e) {
            log.error("Gemini SDK 호출 중 에러 발생: {}", e.getMessage());
            throw new GeneralException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
