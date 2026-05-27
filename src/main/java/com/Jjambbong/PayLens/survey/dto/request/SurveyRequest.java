package com.Jjambbong.PayLens.survey.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SurveyRequest {

    @NotNull(message = "5인 이상 사업장 여부를 입력해주세요.")
    @Schema(description = "같이 일하는 직원이 사장님을 빼고 5명이 넘나요?", example = "true")
    private Boolean isOverFiveEmployees;

    @NotNull(message = "주 15시간 이상 근무 여부를 입력해주세요.")
    @Schema(description = "일주일에 15시간 이상 일하시나요?", example = "true")
    private Boolean isWorkingOverFifteenHours;

    @NotNull(message = "1년 이상 근무 여부를 입력해주세요.")
    @Schema(description = "이 가게에서 일한 지 1년이 넘으셨나요?", example = "false")
    private Boolean isWorkingOverOneYear;

    @NotNull(message = "휴업수당 대상 여부를 입력해주세요.")
    @Schema(description = "사장님이 갑자기 쉬라고 해서 일하러 가지 못한 날이 있나요?", example = "false")
    private Boolean hasUnscheduledDayOff;
}
