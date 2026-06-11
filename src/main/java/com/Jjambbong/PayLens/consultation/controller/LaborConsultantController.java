package com.Jjambbong.PayLens.consultation.controller;

import com.Jjambbong.PayLens.consultation.dto.LaborConsultantRequest;
import com.Jjambbong.PayLens.consultation.dto.LaborConsultantResponse;
import com.Jjambbong.PayLens.consultation.dto.LaborConsultantUpdateRequest;
import com.Jjambbong.PayLens.consultation.service.LaborConsultantService;
import com.Jjambbong.PayLens.global.api.ApiResponse;
import com.Jjambbong.PayLens.global.api.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Labor Consultant API", description = "제휴 노무사 관리(CRUD) API")
@RestController
@RequestMapping("/api/consultants")
@RequiredArgsConstructor
public class LaborConsultantController {

    private final LaborConsultantService laborConsultantService;

    /**
     * 노무사 등록 API
     */
    @Operation(summary = "노무사 등록", description = "새로운 제휴 노무사 정보를 PayLens 시스템에 등록합니다.")
    @PostMapping
    public ApiResponse<Long> createConsultant(@Valid @RequestBody LaborConsultantRequest request) {
        Long consultantId = laborConsultantService.registerConsultant(request);
        // ResponseEntity 대신 ApiResponse.onSuccess 사용
        // result 자리에 생성된 ID를 넣어줍니다.
        return ApiResponse.onSuccess(SuccessCode.OK, consultantId);
    }

    /**
     * 전체 노무사 목록 조회 API
     */
    @Operation(summary = "노무사 전체 목록 조회", description = "시스템에 등록된 모든 제휴 노무사 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<List<LaborConsultantResponse>> getAllConsultants() {
        List<LaborConsultantResponse> responses = laborConsultantService.getAllConsultants();
        // result 자리에 목록(responses)을 넣어줍니다.
        return ApiResponse.onSuccess(SuccessCode.OK, responses);
    }

    /**
     * 특정 노무사 상세 조회 API
     */
    @Operation(summary = "노무사 단건 상세 조회", description = "특정 노무사의 상세 정보를 ID를 통해 조회합니다.")
    @GetMapping("/{id}")
    public ApiResponse<LaborConsultantResponse> getConsultantById(@PathVariable("id") Long id) {
        LaborConsultantResponse response = laborConsultantService.getConsultantById(id);
        // result 자리에 단건 정보(response)를 넣어줍니다.
        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }

    /**
     * 노무사 정보 수정 API
     */
    @Operation(summary = "노무사 정보 수정", description = "특정 노무사의 정보를 수정(업데이트)합니다.")
    @PutMapping("/{id}")
    public ApiResponse<Object> updateConsultant(
            @PathVariable("id") Long id,
            @Valid @RequestBody LaborConsultantUpdateRequest request) {
        laborConsultantService.updateConsultant(id, request);
        // 반환할 데이터가 없으므로 result 자리에 null을 전달합니다.
        return ApiResponse.onSuccess(SuccessCode.OK, null);
    }

    /**
     * 노무사 삭제 API
     */
    @Operation(summary = "노무사 삭제 (소프트 딜리트)", description = "특정 노무사의 활동 상태를 비활성화(INACTIVE) 처리하여 안전하게 삭제합니다.")
    @DeleteMapping("/{id}")
    public ApiResponse<Object> deleteConsultant(@PathVariable("id") Long id) {
        laborConsultantService.deleteConsultant(id);
        // 반환할 데이터가 없으므로 result 자리에 null을 전달합니다.
        return ApiResponse.onSuccess(SuccessCode.OK, null);
    }
}