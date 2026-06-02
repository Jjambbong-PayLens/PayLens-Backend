# ⚖️ PayLens (페이렌즈)
> **외국인 근로자를 위한 AI 기반 임금체불 분석 및 권리 구제 지원 플랫폼**

<p align="center">
  <img src="https://img.shields.io/badge/Java-ED8B00?style=flat-square&logo=openjdk&logoColor=white" alt="Java"/>
  <img src="https://img.shields.io/badge/Spring_Boot-6DB33F?style=flat-square&logo=springboot&logoColor=white" alt="SpringBoot"/>
  <img src="https://img.shields.io/badge/MySQL-4479A1?style=flat-square&logo=mysql&logoColor=white" alt="MySQL"/>
  <img src="https://img.shields.io/badge/AWS-CloudFront%20%7C%20S3%20%7C%20EC2-232F3E?style=flat-square&logo=amazon-aws&logoColor=white" alt="AWS"/>
</p>

---

## 🚀 서비스 개요 (Service Overview)

**PayLens**는 언어 장벽과 복잡한 노동법 체계로 인해 임금체불 피해에 취약한 **외국인 근로자를 위한 자가 진단 및 권리 구제 플랫폼**입니다. 

근로자가 업로드한 급여명세서와 근로계약서 등 정형/비정형 문서 데이터를 기반으로 법정 수당(기본급, 연장수당, 야간수당, 휴일수당, 주휴수당 등)의 정합성을 AI로 교차 검증하고, 체불 예상 금액 및 증거 분석 리포트를 도출하여 실질적인 노무 상담 및 고용노동부 진정 제기 단계까지 매끄럽게 연결합니다.

---

## ✨ 핵심 기능 (Key Features)

- **간편한 소셜 인증**: 구글 및 카카오 OAuth 2.0 기반의 소셜 로그인을 제공하여 복잡한 회원가입 절차 없이 간편하게 접근할 수 있습니다.
- **법적 근거 마련을 위한 4대 핵심 문진 (Smart Survey)**: 사업장 상시 근로자 수(5인 이상 여부), 주 소정근로시간, 근속 기간, 휴업수당 대상 여부 등 까다로운 법정 수당 가산 조건들을 데이터베이스(`surveys`)에 직접 구조화하여 AI 프롬프트에 동적으로 반영합니다.
- **다중 비정형 데이터 업로드**: AWS S3 인프라와 Pre-signed URL 아키텍처를 결합하여 대용량 급여 자료나 증빙 문서를 안전하고 빠르게 다중 업로드할 수 있습니다.
- **프리미엄 페이월 및 이력 관리 (Premium Subscription)**: 월 4,900원 정기 결제 비즈니스 모델(BM)을 탑재하여 구독 유저에게 상세 분석 대시보드와 다운로드 가능한 정식 PDF 리포트 발급 권한을 제어합니다. 과거 분석 이력은 마이페이지 서류 보관함을 통해 언제든 재조회할 수 있습니다.
- **전문가 연계 인프라 (Post-Office Model)**: 공인노무사법 리스크를 철저히 방어하기 위해 플랫폼이 중개 수수료를 취하지 않는 순수 편리 도구로 작동하며, 유저의 동의하에 한국어 법률 PDF 리포트를 제휴 노무사에게 직통 발송합니다.

---

## 🛠 기술 스택 (Tech Stack)

## Backend
|<img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/java/java-original.svg" width="50" height="50" />|<img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/spring/spring-original.svg" width="50" height="50" />|<img src="https://cdn.simpleicons.org/springsecurity/6DB33F" width="50" height="50" />|<img src="https://cdn.simpleicons.org/jsonwebtokens/000000" width="50" height="50" />|
|:---:|:---:|:---:|:---:|
|Java|Spring Boot|Spring Security|JWT|

## Data & Storage
|<img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/mysql/mysql-original.svg" width="50" height="50" />|<img src="https://cdn.simpleicons.org/hibernate/59666C" width="50" height="50" />|
|:---:|:---:|
|MySQL|Spring Data JPA|

## AI Engine & Cloud Infrastructure
|<img src="https://cdn.simpleicons.org/googlegemini/8E75B2" width="50" height="50" />|<img src="https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/amazonwebservices/amazonwebservices-original-wordmark.svg" width="50" height="50" />|
|:---:|:---:|
|Google Gemini API|AWS|

## API & Tools
|<img src="https://cdn.simpleicons.org/swagger/85EA2D" width="50" height="50" />|<img src="https://cdn.simpleicons.org/gmail/EA4335" width="50" height="50" />|
|:---:|:---:|
|Swagger UI|Spring Mail (SMTP)|

---

## 🔗 ERD

<img width="2360" height="1482" alt="PayLens" src="https://github.com/user-attachments/assets/2e996bb2-2fb4-4705-88a0-d6118764a399" />

---

## 🏗 시스템 아키텍처 (System Architecture)

<img width="569" height="327" alt="image" src="https://github.com/user-attachments/assets/ef05415a-d651-4674-b9dc-a3e4691b0a80" />


---

## 🤝 프로토타입
<img width="1028" height="1054" alt="image" src="https://github.com/user-attachments/assets/a823626f-642a-4f1c-8f32-38fd49ca0906" />

---

## 👥 팀원 및 역할 (Team Jjambbong)

| 김동현 | 김규현 | 최민서 | 주민서 |
| :---: | :---: | :---: | :---: |
| <img src="[https://github.com/dh1180.png](https://github.com/dh1180.png)" width="100"> | <img src="[https://github.com/github.png](https://github.com/github.png)" width="100"> | <img src="[https://github.com/github.png](https://github.com/github.png)" width="100"> | <img src="[https://github.com/github.png](https://github.com/github.png)" width="100"> |
| [@dh1180](https://github.com/dh1180) | [@github](https://github.com) | [@github](https://github.com) | [@github](https://github.com) |
| **Project Leader / BE** | **BE** | **AI** | **BE** |
|||||
---
