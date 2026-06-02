package com.Jjambbong.PayLens.consultation.controller;

import com.Jjambbong.PayLens.consultation.dto.LaborConsultantRequest;
import com.Jjambbong.PayLens.consultation.dto.LaborConsultantResponse; // 👉 응답용 DTO import (새로 추가)
import com.Jjambbong.PayLens.consultation.service.LaborConsultantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/consultants")
@RequiredArgsConstructor
public class LaborConsultantController {

    private final LaborConsultantService laborConsultantService;

    /**
     * 노무사 등록 API
     * POST /api/consultants
     */
    @PostMapping
    public ResponseEntity<Long> createConsultant(@Valid @RequestBody LaborConsultantRequest request) {
        // 1. Service로 데이터를 넘겨서 비즈니스 로직(DB 저장) 수행
        Long consultantId = laborConsultantService.registerConsultant(request);

        // 2. 저장이 완료되면 생성된 노무사의 ID와 함께 201 Created 상태 코드를 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(consultantId);
    }

    /**
     * 전체 노무사 목록 조회 API
     * GET /api/consultants
     */
    @GetMapping
    public ResponseEntity<List<LaborConsultantResponse>> getAllConsultants() {
        // 1. Service를 호출하여 모든 노무사 목록 DTO를 가져옵니다.
        List<LaborConsultantResponse> responses = laborConsultantService.getAllConsultants();

        // 2. 200 OK 상태 코드와 함께 목록 데이터를 반환합니다.
        return ResponseEntity.ok(responses);
    }

    /**
     * 특정 노무사 상세 조회 API
     * GET /api/consultants/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<LaborConsultantResponse> getConsultantById(@PathVariable("id") Long id) {
        // 1. URL 경로에 있는 id 값을 Service로 넘겨서 상세 정보를 가져옵니다.
        LaborConsultantResponse response = laborConsultantService.getConsultantById(id);

        // 2. 200 OK 상태 코드와 함께 상세 데이터를 반환합니다.
        return ResponseEntity.ok(response);
    }
}