# API 명세서

> 출처: Notion 「API 명세서」 (마지막 수정: 2026-11-11)

엔드포인트별 상세(요청·응답 예시)는 도메인별 파일로 나누어 두었다.

| 도메인 | 파일 | 포함 API |
| --- | --- | --- |
| 회원·인증 | [api/auth.md](api/auth.md) | API-AUTH-01 ~ 05 |
| 정책 | [api/policy.md](api/policy.md) | API-ADM-01 ~ 03 |
| 제재 | [api/sanction.md](api/sanction.md) | API-ADM-04 ~ 07 |
| FAQ | [api/faq.md](api/faq.md) | API-ADM-08 ~ 11 |
| 상담·채팅 | [api/chat.md](api/chat.md) | API-001 ~ 008 |
| 고객 상담·AI 상담 | [api/customer-chat.md](api/customer-chat.md) | CUS-01 ~ 05 |
| 상담 이력·통계 | [api/history.md](api/history.md) | HIS-01 ~ 07 |
| 폭언 감지·경고 | [api/moderation.md](api/moderation.md) | MOD-01 ~ 07 |
| 내부 이벤트·연동 | [api/internal.md](api/internal.md) | INT-01 ~ 02 |

## API 목록

| API ID | 분류 | Method | URL | 기능명 | 인증 | 상태 | 담당자 | 비고 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| [API-AUTH-01](api/auth.md#api-auth-01-회원가입) | 회원 | POST | `/api/auth/signup` | 회원가입 | 불필요 | 작성완료 | 장건웅 |  |
| [API-AUTH-02](api/auth.md#api-auth-02-아이디-중복-확인) | 회원 | GET | `/api/auth/check-login-id` | 아이디 중복 확인 | 불필요 | 작성완료 | 장건웅 |  |
| [API-AUTH-03](api/auth.md#api-auth-03-로그인) | 인증 | POST | `/api/auth/login` | 로그인 | 불필요 | 작성완료 | 장건웅 |  |
| [API-AUTH-04](api/auth.md#api-auth-04-로그아웃) | 인증 | POST | `/api/auth/logout` | 로그아웃 | 필요 | 작성완료 | 장건웅 |  |
| [API-AUTH-05](api/auth.md#api-auth-05-내-정보-조회) | 인증 | GET | `/api/auth/me` | 내 정보 조회 | 필요 | 작성완료 | 장건웅 |  |
| [API-ADM-01](api/policy.md#api-adm-01-제재-정책-조회) | 정책 | GET | `/api/admin/policies` | 제재 정책 조회 | 필요 (ADMIN) | 작성완료 | 장건웅 |  |
| [API-ADM-02](api/policy.md#api-adm-02-제재-정책-수정) | 정책 | PUT | `/api/admin/policies` | 제재 정책 수정 | 필요 (ADMIN) | 작성완료 | 장건웅 |  |
| [API-ADM-03](api/policy.md#api-adm-03-정책-변경-이력-조회) | 정책 | GET | `/api/admin/policies/histories` | 정책 변경 이력 조회 | 필요 (ADMIN) | 작성완료 | 장건웅 |  |
| [API-ADM-04](api/sanction.md#api-adm-04-제재-목록-조회) | 제재 | GET | `/api/admin/sanctions` | 제재 목록 조회 | 필요 (ADMIN) | 작성완료 | 장건웅 |  |
| [API-ADM-05](api/sanction.md#api-adm-05-제재-상세-조회) | 제재 | GET | `/api/admin/sanctions/{sanctionId}` | 제재 상세 조회 | 필요 (ADMIN) | 작성완료 | 장건웅 |  |
| [API-ADM-06](api/sanction.md#api-adm-06-제재-수동-해제) | 제재 | POST | `/api/admin/sanctions/{sanctionId}/release` | 제재 수동 해제 | 필요 (ADMIN) | 작성완료 | 장건웅 |  |
| [API-ADM-07](api/sanction.md#api-adm-07-위반-원문-조회) | 제재 | GET | `/api/admin/violations/{violationId}/original` | 위반 원문 조회 | 필요 (ADMIN) | 작성완료 | 장건웅 |  |
| [API-ADM-08](api/faq.md#api-adm-08-faq-목록-조회) | FAQ | GET | `/api/admin/faqs` | FAQ 목록 조회 | 필요 (ADMIN) | 작성완료 | 장건웅 |  |
| [API-ADM-09](api/faq.md#api-adm-09-faq-등록) | FAQ | POST | `/api/admin/faqs` | FAQ 등록 | 필요 (ADMIN) | 작성완료 | 장건웅 | could |
| [API-ADM-10](api/faq.md#api-adm-10-faq-수정) | FAQ | PUT | `/api/admin/faqs/{faqId}` | FAQ 수정 | 필요 (ADMIN) | 작성완료 | 장건웅 | could |
| [API-ADM-11](api/faq.md#api-adm-11-faq-삭제) | FAQ | DELETE | `/api/admin/faqs/{faqId}` | FAQ 삭제 | 필요 (ADMIN) | 작성완료 | 장건웅 | could |
| [API-001](api/chat.md#api-001-상담-대기열-조회) | 상담 | GET | `/api/v1/chats/queue` | 상담 대기열 조회 | 필요 | 수정완료 | 정혜영 |  |
| [API-002](api/chat.md#api-002-상담-수락) | 상담 | PATCH | `/api/v1/chats/{chatId}/accept` | 상담 수락 | 필요 | 수정완료 | 정혜영 |  |
| [API-003](api/chat.md#api-003-상담-상세-조회) | 상담 | GET | `/api/v1/chats/{chatId}` | 상담 상세 조회 | 필요 | 수정완료 | 정혜영 |  |
| [API-004](api/chat.md#api-004-상담-메시지-조회) | 채팅 | GET | `/api/v1/chats/{chatId}/messages` | 상담 메시지 조회 | 필요 | 수정완료 | 정혜영 |  |
| [API-005](api/chat.md#api-005-실시간-메시지-전송) | 채팅 | WebSocket | `/app/chats/{chatId}/messages` | 실시간 메시지 전송 | 필요 | 수정완료 | 정혜영 |  |
| [API-006](api/chat.md#api-006-상담-종료) | 상담 | PATCH | `/api/v1/chats/{chatId}/close` | 상담 종료 | 필요 | 수정완료 | 정혜영 |  |
| [API-007](api/chat.md#api-007-ai-응대-내역-조회) | 상담 | GET | `/api/v1/chats/{chatId}/ai-messages` | AI 응대 내역 조회 | 필요 | 수정완료 | 정혜영 |  |
| [API-008](api/chat.md#api-008-상담원-회복-시간-조회) | 상담원 | GET | `/api/v1/agents/me/recovery` | 상담원 회복 시간 조회 | 필요 | 수정완료 | 정혜영 |  |
| [CUS-01](api/customer-chat.md#cus-01-상담-시작) | 상담 | POST | `/api/v1/chats` | 상담 시작 | 필요 (CUSTOMER) | 작성완료 | 심소민 | 정지 고객 차단 포함 |
| [CUS-02](api/customer-chat.md#cus-02-ai-상담-메시지-전송응답) | AI 상담 | POST | `/api/v1/chats/{chatId}/bot-messages` | AI 상담 메시지 전송·응답 | 필요 (CUSTOMER) | 작성완료 | 심소민 | AI 실패 시 FAQ 대체 |
| [CUS-03](api/customer-chat.md#cus-03-상담원-연결-요청) | AI 상담 | PATCH | `/api/v1/chats/{chatId}/handoff` | 상담원 연결 요청 | 필요 (CUSTOMER) | 작성완료 | 심소민 | BOT → QUEUED |
| [CUS-04](api/customer-chat.md#cus-04-고객-상담-종료-ai-응대대기-중) | 상담 | PATCH | `/api/v1/chats/{chatId}/cancel` | 고객 상담 종료 (AI 응대·대기 중) | 필요 (CUSTOMER) | 작성완료 | 심소민 | ConsultationClosedEvent 발행 |
| [CUS-05](api/customer-chat.md#cus-05-ai-목업-모드-변경) | AI 상담 | PATCH | `/api/v1/dev/ai-mock-mode` | AI 목업 모드 변경 | 필요 (ADMIN) | 작성완료 | 심소민 | should, 시연용 |
| [HIS-01](api/history.md#his-01-내-상담-이력-목록) | 상담 이력 | GET | `/api/customers/me/consultations` | 내 상담 이력 목록 | 필요 | 작성완료 | 유능한 |  |
| [HIS-02](api/history.md#his-02-내-이용-상태제재-조회) | 상담 이력 | GET | `/api/customers/me/sanction` | 내 이용 상태(제재) 조회 | 필요 | 작성완료 | 유능한 |  |
| [HIS-03](api/history.md#his-03-응대-고객별-이력) | 상담 이력 | GET | `/api/agents/me/customers` | 응대 고객별 이력 | 필요 | 작성완료 | 유능한 |  |
| [HIS-04](api/history.md#his-04-내가-맡은-상담-목록) | 상담 이력 | GET | `/api/agents/me/consultations` | 내가 맡은 상담 목록 | 필요 | 작성완료 | 유능한 |  |
| [HIS-05](api/history.md#his-05-전체-상담-이력) | 상담 이력 | GET | `/api/admin/consultations` | 전체 상담 이력 | 필요 | 작성완료 | 유능한 |  |
| [HIS-06](api/history.md#his-06-상담-상세) | 상담 이력 | GET | `/api/consultations/{consultationId}/history` | 상담 상세 | 필요 | 작성완료 | 유능한 |  |
| [HIS-07](api/history.md#his-07-운영-대시보드) | 통계 | GET | `/api/admin/dashboard` | 운영 대시보드 | 필요 | 작성완료 | 유능한 |  |
| [MOD-01](api/moderation.md#mod-01-폭언-감지-및-메시지-마스킹) | 내부 | INTERNAL | `ModerationService.processMessage()` | 폭언 감지 및 메시지 마스킹 | 불필요 (서버 내부 호출, 고객 메시지 송신 시 JWT 인증은 선행) | 작성완료 | 한수민 |  |
| [MOD-02](api/moderation.md#mod-02-자동-경고-처리) | 내부 | INTERNAL | `ModerationService.applyAutoWarning()` | 자동 경고 처리 | 불필요 (서버 내부 호출) | 작성완료 | 한수민 |  |
| [MOD-03](api/moderation.md#mod-03-폭언-누적-상담-자동-종료) | 내부 | INTERNAL | `ModerationService.terminateForAbuse()` | 폭언 누적 상담 자동 종료 | 불필요 (서버 내부 이벤트) | 작성완료 | 한수민 |  |
| [MOD-04](api/moderation.md#mod-04-위반-기록-및-자동-제재) | 내부 | EVENT | `AbuseConsultationTerminatedEvent` | 위반 기록 및 자동 제재 | 불필요(서버 내부) | 작성완료 | 한수민 |  |
| [MOD-05](api/moderation.md#mod-05-상담별-경고-내역-조회) | 상담 | GET | `/api/consultations/{consultationId}/warnings` | 상담별 경고 내역 조회 | 필요 (AGENT, ADMIN) | 작성완료 | 한수민 |  |
| [MOD-06](api/moderation.md#mod-06-상담원-수동-경고-발송) | 상담 | POST | `/api/consultations/{consultationId}/warnings` | 상담원 수동 경고 발송 | 필요 (AGENT) | 작성완료 | 한수민 |  |
| [MOD-07](api/moderation.md#mod-07-상담원-수동-강제-종료) | 상담 | PATCH | `/api/consultations/{consultationId}/terminate` | 상담원 수동 강제 종료 | 필요 (AGENT, ADMIN) | 작성완료 | 한수민 |  |
| [INT-01](api/internal.md#int-01-상담-요약-생성) | 내부 | EVENT | `ConsultationClosedEvent` | 상담 요약 생성 | 불필요(서버 내부) | 작성완료 | 유능한 |  |
| [INT-02](api/internal.md#int-02-ai-게이트웨이) | 내부 | INTERNAL | `AiGateway.reply()` | AI 게이트웨이 | 불필요(서버 내부) | 작성완료 | 심소민 | 타임아웃 10초·mock |

## 작성 시 주의사항

- URL은 리소스(명사) 중심으로 작성한다.
- HTTP Method는 조회 GET, 등록 POST, 전체 수정 PUT, 일부 수정 PATCH, 삭제 DELETE를 기준으로 사용한다.
- 인증이 필요한 API는 Authorization 헤더를 반드시 작성한다.
- Request Body와 Response Body는 실제 프론트엔드에서 사용할 JSON 구조와 맞춘다.
- 상태 코드는 성공과 실패 상황을 모두 작성한다.
- API 명세서가 변경되면 화면-API 매핑표와 백엔드 Controller도 함께 수정해야 한다.
