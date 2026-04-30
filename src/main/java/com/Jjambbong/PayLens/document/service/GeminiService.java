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

        String targetLanguage = user.getPreferredLanguage().getDescription();
        String prompt = String.format(
                "첨부된 급여명세서(또는 문서)를 분석해서 주요 항목과 금액을 JSON 형식으로 추출해줘. " +
                        "반드시 모든 키와 값의 언어는 '%s'로 작성해야 해. JSON 외의 다른 설명은 하지 마.",
                targetLanguage
        );

        try {
            // application.yml의 값을 명시적으로 주입
            Client client = Client.builder()
                    .apiKey(geminiApiKey)
                    .build();

            Part textPart = Part.builder().text(prompt).build();
            Part pdfPart = Part.builder()
                    .inlineData(Blob.builder()
                            .mimeType("application/pdf")
                            .data(base64Pdf) // 변환 없이 String 그대로 삽입
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