package com.Jjambbong.PayLens.survey.domain;

import com.Jjambbong.PayLens.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "surveys")
public class Survey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // 1. 같이 일하는 직원이 사장님을 빼고 5명이 넘나요?
    @Column(nullable = false)
    private boolean isOverFiveEmployees;

    // 2. 일주일에 15시간 이상 일하시나요?
    @Column(nullable = false)
    private boolean isWorkingOverFifteenHours;

    // 3. 이 가게에서 일한 지 1년이 넘으셨나요?
    @Column(nullable = false)
    private boolean isWorkingOverOneYear;

    // 4. 사장님이 갑자기 쉬라고 해서 일하러 가지 못한 날이 있나요?
    @Column(nullable = false)
    private boolean hasUnscheduledDayOff;

    @Builder
    public Survey(User user, boolean isOverFiveEmployees, boolean isWorkingOverFifteenHours, boolean isWorkingOverOneYear, boolean hasUnscheduledDayOff) {
        this.user = user;
        this.isOverFiveEmployees = isOverFiveEmployees;
        this.isWorkingOverFifteenHours = isWorkingOverFifteenHours;
        this.isWorkingOverOneYear = isWorkingOverOneYear;
        this.hasUnscheduledDayOff = hasUnscheduledDayOff;
    }

    public void updateSurvey(boolean isOverFiveEmployees, boolean isWorkingOverFifteenHours, boolean isWorkingOverOneYear, boolean hasUnscheduledDayOff) {
        this.isOverFiveEmployees = isOverFiveEmployees;
        this.isWorkingOverFifteenHours = isWorkingOverFifteenHours;
        this.isWorkingOverOneYear = isWorkingOverOneYear;
        this.hasUnscheduledDayOff = hasUnscheduledDayOff;
    }
}
