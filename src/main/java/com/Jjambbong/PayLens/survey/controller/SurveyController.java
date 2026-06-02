package com.Jjambbong.PayLens.survey.controller;

import com.Jjambbong.PayLens.global.api.ApiResponse;
import com.Jjambbong.PayLens.global.api.SuccessCode;
import com.Jjambbong.PayLens.survey.dto.request.SurveyRequest;
import com.Jjambbong.PayLens.survey.service.SurveyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Survey", description = "사용자 문진표 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/surveys")
public class SurveyController {

    private final SurveyService surveyService;

    @PostMapping
    @Operation(summary = "문진 내역 저장 및 수정 API", description = "사용자의 4대 문진 결과를 저장하거나 수정합니다.")
    public ApiResponse<Object> saveOrUpdateSurvey(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Valid @RequestBody SurveyRequest surveyRequest) {

        surveyService.saveOrUpdateSurvey(userId, surveyRequest);

        return ApiResponse.onSuccess(SuccessCode.USER_SURVEY_UPDATE_SUCCESS, null);
    }
}
