package com.Jjambbong.PayLens.survey.service;

import com.Jjambbong.PayLens.global.api.ErrorCode;
import com.Jjambbong.PayLens.global.exception.GeneralException;
import com.Jjambbong.PayLens.survey.domain.Survey;
import com.Jjambbong.PayLens.survey.dto.request.SurveyRequest;
import com.Jjambbong.PayLens.survey.repository.SurveyRepository;
import com.Jjambbong.PayLens.user.domain.User;
import com.Jjambbong.PayLens.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class SurveyService {

    private final SurveyRepository surveyRepository;
    private final UserRepository userRepository;

    public void saveOrUpdateSurvey(Long userId, SurveyRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        Optional<Survey> existingSurvey = surveyRepository.findByUser(user);

        if (existingSurvey.isPresent()) {
            // 이미 문진 내역이 있으면 업데이트
            existingSurvey.get().updateSurvey(
                    request.getIsOverFiveEmployees(),
                    request.getIsWorkingOverFifteenHours(),
                    request.getIsWorkingOverOneYear(),
                    request.getHasUnscheduledDayOff()
            );
        } else {
            // 없으면 새로 생성
            Survey survey = Survey.builder()
                    .user(user)
                    .isOverFiveEmployees(request.getIsOverFiveEmployees())
                    .isWorkingOverFifteenHours(request.getIsWorkingOverFifteenHours())
                    .isWorkingOverOneYear(request.getIsWorkingOverOneYear())
                    .hasUnscheduledDayOff(request.getHasUnscheduledDayOff())
                    .build();
            surveyRepository.save(survey);
        }
    }
}
