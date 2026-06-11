package com.Jjambbong.PayLens.consultation.service;

import com.Jjambbong.PayLens.consultation.domain.ConsultationRequest;
import com.Jjambbong.PayLens.consultation.domain.LaborConsultant;
import com.Jjambbong.PayLens.consultation.repository.ConsultationRequestRepository;
import com.Jjambbong.PayLens.consultation.repository.LaborConsultantRepository;
// 🌟 유저 엔티티와 레포지토리 임포트 추가
import com.Jjambbong.PayLens.user.domain.User;
import com.Jjambbong.PayLens.user.repository.UserRepository;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.Jjambbong.PayLens.consultation.domain.ConsultationRequest;
import com.Jjambbong.PayLens.consultation.domain.LaborConsultant;
import com.Jjambbong.PayLens.consultation.repository.ConsultationRequestRepository;
import com.Jjambbong.PayLens.consultation.repository.LaborConsultantRepository;
import com.Jjambbong.PayLens.user.domain.User;
import com.Jjambbong.PayLens.user.repository.UserRepository;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsultationService {

    private final LaborConsultantRepository consultantRepository;
    private final ConsultationRequestRepository requestRepository;
    private final UserRepository userRepository; //유저 정보를 찾기 위해 추가
    private final JavaMailSender mailSender;

    /**
     * 노무사 상담 신청 및 리포트 메일 발송
     */
    @Transactional
    public void requestConsultation(Long userId, Long consultantId, MultipartFile pdfFile) {

        // 1. 요청한 유저 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        // 2. 매칭할 노무사 정보 조회
        LaborConsultant consultant = consultantRepository.findById(consultantId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 노무사입니다."));

        // 3. DB에 상담 신청 기록 생성
        ConsultationRequest request = ConsultationRequest.builder()
                .userId(userId)
                .laborConsultant(consultant)
                .reportFileUrl(pdfFile.getOriginalFilename())
                .build();
        requestRepository.save(request);

        // 4. 노무사 이메일 주소로 PDF 첨부 메일 발송 (유저 이메일도 같이 넘겨줌)
        try {
            sendEmailWithAttachment(consultant.getEmail(), user.getEmail(), pdfFile);
            request.completeRequest();
        } catch (Exception e) {
            request.failRequest();
            throw new RuntimeException("노무사 메일 발송 중 오류가 발생했습니다. 다시 시도해 주세요.", e);
        }
    }

    /**
     * 실제 메일을 조립하고 발송하는 프라이빗 메서드
     * @param toEmail 노무사 이메일
     * @param replyToEmail 유저 개인 이메일 (회신처)
     * @param file 리포트 PDF 파일
     */
    private void sendEmailWithAttachment(String toEmail, String replyToEmail, MultipartFile file) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(toEmail);

        // 회신처(Reply-To)를 유저의 이메일로 지정
        helper.setReplyTo(replyToEmail);

        helper.setSubject("[PayLens] 유료 유저 이상탐지 리포트 상담 요청 건");
        helper.setText("안녕하세요, 제휴 노무사님.\n\nPayLens 서비스로부터 새로운 상담 요청이 접수되었습니다.\n" +
                "유저가 발송한 이상탐지 리포트 PDF 파일을 첨부하오니 확인 후 검토 부탁드립니다.\n\n" +
                "※ 본 메일에 '답장'을 누르시면, 상담을 요청한 유저에게 바로 메일이 발송됩니다.\n\n" +
                "감사합니다.\nPayLens 팀 드림.");

        helper.addAttachment(file.getOriginalFilename(), file);
        mailSender.send(message);
    }
}