package com.Jjambbong.PayLens.consultation.service;

import com.Jjambbong.PayLens.consultation.domain.LaborConsultant;
import com.Jjambbong.PayLens.consultation.dto.LaborConsultantRequest;
import com.Jjambbong.PayLens.consultation.dto.LaborConsultantResponse; // 👉 응답용 DTO import (새로 추가)
import com.Jjambbong.PayLens.consultation.dto.LaborConsultantUpdateRequest;
import com.Jjambbong.PayLens.consultation.repository.LaborConsultantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LaborConsultantService {

    private final LaborConsultantRepository laborConsultantRepository;

    /**
     * 노무사 등록 (Create)
     */
    @Transactional
    public Long registerConsultant(LaborConsultantRequest request) {
        // 1. 클라이언트에게 받은 DTO 데이터를 Entity로 변환
        LaborConsultant consultant = LaborConsultant.builder()
                .name(request.getName())
                .officeName(request.getOfficeName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .kakaoChannel(request.getKakaoChannel())
                .region(request.getRegion())
                .specialties(request.getSpecialties())
                .supportedLanguages(request.getSupportedLanguages())
                .introduction(request.getIntroduction())
                .status("ACTIVE") // 기본 활동 상태
                .build();

        // 2. Repository를 통해 DB에 저장
        LaborConsultant savedConsultant = laborConsultantRepository.save(consultant);

        // 3. 정상적으로 저장되었다면, 생성된 노무사의 ID(PK)를 반환
        return savedConsultant.getId();
    }

    /**
     * 전체 노무사 목록 조회 (Read)
     */
    @Transactional(readOnly = true)
    public List<LaborConsultantResponse> getAllConsultants() {
        // 1. DB에서 모든 노무사 Entity 조회
        List<LaborConsultant> consultants = laborConsultantRepository.findAll();

        // 2. Entity 리스트를 DTO 리스트로 변환하여 반환 (Java Stream API 사용)
        return consultants.stream()
                .map(LaborConsultantResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * 특정 노무사 상세 조회 (Read One)
     */
    @Transactional(readOnly = true)
    public LaborConsultantResponse getConsultantById(Long id) {
        // 1. Repository에서 ID로 노무사를 찾습니다.
        // 만약 해당 ID가 없으면 예외(에러)를 발생시킵니다.
        LaborConsultant consultant = laborConsultantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 노무사를 찾을 수 없습니다. ID: " + id));

        // 2. 찾은 Entity를 DTO로 변환해서 반환합니다.
        return new LaborConsultantResponse(consultant);
    }

    /**
     * 노무사 정보 수정 (Update)
     */
    @Transactional
    public void updateConsultant(Long id, LaborConsultantUpdateRequest request) {
        // 1. 수정할 노무사를 ID로 조회 (없으면 예외 발생)
        LaborConsultant consultant = laborConsultantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 노무사를 찾을 수 없습니다. ID: " + id));

        // 2. 조회해 온 Entity의 내용을 수정 요청 데이터로 변경
        // (Entity 클래스에 이 데이터를 변경하는 메서드를 추가해 줄 것입니다!)
        consultant.update(
                request.getName(),
                request.getOfficeName(),
                request.getPhone(),
                request.getEmail(),
                request.getKakaoChannel(),
                request.getRegion(),
                request.getSpecialties(),
                request.getSupportedLanguages(),
                request.getIntroduction(),
                request.getStatus()
        );
    }

    /**
     * 노무사 삭제/비활성화 (Delete)
     */
    @Transactional
    public void deleteConsultant(Long id) {
        // 1. 삭제할 노무사를 ID로 조회 (없으면 예외 발생)
        LaborConsultant consultant = laborConsultantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 노무사를 찾을 수 없습니다. ID: " + id));

        // 2. 완전히 지우는 대신, 활동 상태를 'INACTIVE'로 변경하여 노출되지 않도록 합니다.
        consultant.update(
                consultant.getName(),
                consultant.getOfficeName(),
                consultant.getPhone(),
                consultant.getEmail(),
                consultant.getKakaoChannel(),
                consultant.getRegion(),
                consultant.getSpecialties(),
                consultant.getSupportedLanguages(),
                consultant.getIntroduction(),
                "INACTIVE" //상태를 비활성화로 변경
        );
    }
}