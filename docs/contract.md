# 팀 간 약속 (Contract)

> 두 명 이상이 같이 쓰는 값과 형식을 모은 문서다. enum, URL, 응답 형식, errorCode, 종료 처리, 내부 이벤트, WebSocket 메시지가 여기 있다.
> 각 API의 요청·응답 상세는 [api/](api/README.md), 동작 규칙과 숫자 기준은 [policy.md](policy.md)를 본다.
> 여기 있는 값은 혼자 바꾸지 않는다. 바꿔야 하면 아래 「확정 필요」에 올리고 Daily Scrum(09:30, 17:20)에서 정한다.

## 확정 필요

문서끼리 값이 달라 개발 전에 하나로 정해야 하는 항목이다. **제안값은 확정이 아니다.** 확정되면 상태를 바꾸고, 제안과 다른 쪽 문서를 같은 PR에서 고친다.

| ID | 항목 | 지금 문서 상태 | 제안 | 영향 | 상태 |
| --- | --- | --- | --- | --- | --- |
| D-01 | 상담 상태 이름 | policy·chat.md: `AI_CHAT`, `IN_PROGRESS` / customer-chat.md·코드 컨벤션: `BOT`, `ACTIVE` | `AI_CHAT`, `QUEUED`, `IN_PROGRESS`, `CLOSED`, `TERMINATED` (정책서 기준, customer-chat.md만 고치면 됨) | 전원 | 제안 |
| D-02 | URL 접두사 | auth·admin·history·moderation: `/api/...` / chat·customer-chat: `/api/v1/...` | `/api`로 통일 (`v1` 제거) | 정혜영, 심소민 | 제안 |
| D-03 | 상담 리소스 경로 | chat·customer-chat: `/chats/{chatId}` / history·moderation: `/consultations/{consultationId}` | URL은 `chats/{chatId}`로 통일. HIS-06, MOD-05~07, 목록 API(`/me/consultations`, `/admin/consultations`)를 `chats`로 바꾼다. Java·DB 이름은 `Consultation` 그대로 | 유능한, 한수민 | 제안 |
| D-04 | 종료 사유·주체 코드 | internal.md: `NORMAL`·`QUEUE_CANCEL`·`ABUSE_AUTO`·`ABUSE_MANUAL` / moderation.md: `ABUSE_THRESHOLD_EXCEEDED`, 자유 문장 / chat.md: `AGENT_CLOSED`(예시) | internal.md 기준 4개 (아래 [종료 처리](#종료-처리)) | 정혜영, 한수민, 유능한 | 제안 |
| D-05 | 메시지 발신자 | customer-chat.md: `BOT` / chat.md: `AI` | `CUSTOMER`, `AGENT`, `BOT`. AI 메시지의 `sender_id`(ERD상 필수 FK) 처리도 같이 정한다 | 심소민, 정혜영, 유능한 | 제안 |
| D-06 | errorCode 이름 | `UNAUTHORIZED`/`AUTH_REQUIRED`, `FORBIDDEN_ROLE`/`ACCESS_DENIED`, `CHAT_NOT_FOUND`/`CONSULTATION_NOT_FOUND`, `CHAT_CLOSED`/`CHAT_ALREADY_CLOSED`, `INVALID_INPUT`/`INVALID_PARAMETER`이 섞여 있음. auth·policy·sanction·faq·chat 문서는 errorCode가 없음 | 아래 [errorCode](#errorcode) 표로 통일 | 전원 | 제안 |
| D-07 | 종료 시각 컬럼 | customer-chat.md·코드 컨벤션: `ended_at` / moderation·history: `closed_at` | `closed_at` (`closed_by`, `close_reason`과 이름을 맞춤) | 유능한 (ERD) | 제안 |
| D-08 | 수동 경고·수동 종료 | policy.md: 수동 경고도 1회로 세고, 폭언 사유 수동 종료는 위반 1회 / moderation.md: 정책 확정 전까지 보류 | policy.md 기준으로 구현 | 한수민 | 제안 |
| D-09 | 폭언 종료 이벤트 | INT-01: 모든 종료는 `ConsultationClosedEvent` 발행 / MOD-03: `AbuseConsultationTerminatedEvent`만 적혀 있음 | 폭언 종료는 두 이벤트를 모두 발행 (아래 [내부 이벤트](#내부-이벤트)) | 한수민, 유능한 | 제안 |
| D-10 | WebSocket 규격 | chat.md API-005 「팀 확인 필요 사항」 12개 미정 (인증, 구독 경로, 오류 전달 등). 고객 대기 화면이 수락을 알 방법이 없음 | 아래 [WebSocket](#websocket) 제안. **WBS 기한 11/11** | 정혜영, 한수민, 장건웅 | 제안 |
| D-11 | 프론트 라우트 | 화면 설계서: `/customer/consultations/:id/chat` 등 / 실제 코드: `/customer/chat`, `/customer/live`, `/admin/policy` | 아래 [화면 라우트](#화면-라우트) 표를 기준으로 확정 | 장건웅 (라우팅), 화면 담당 전원 | 제안 |
| D-12 | AI 호출 기록 | NFR-010: Won't / INT-02: `ai_call_log` 테이블에 저장 | INT-02대로 저장 (실패 시 상담에 영향 없음). NFR-010은 Could로 수정 | 심소민, 유능한 | 제안 |
| D-13 | AI 게이트웨이 위치 | 백엔드 패키지에 자리가 없음 (`faq/`만 있음) | `com.safedesk.ai` 패키지 추가 (게이트웨이·목업), FAQ 매칭은 `faq/` | 심소민 | 제안 |

정리하면서 고친 것 (팀 확인만 필요):

- 마스킹 문자는 `***`로 맞췄다. (REQ-015에 `**`로 적혀 있었음)
- 상태 전이표의 「IN_PROGRESS → CLOSED(폭언 자동 종료)」를 지웠다. 폭언 종료는 `TERMINATED`로만 간다. (모든 API 문서가 이 기준)

## 공통 응답 형식

모든 REST API는 아래 형식으로 응답한다. (NFR-013)

```json
{ "success": true,  "data": { }, "message": "상담이 시작되었습니다." }
{ "success": false, "data": null, "message": "종료된 상담입니다.", "errorCode": "CHAT_CLOSED" }
```

- 실패해도 화면 이동에 필요한 값은 `data`에 담을 수 있다. 예: 이용정지(`reason`, `releaseAt`), 진행 중 상담 있음(`chatId`, `status`)
- AI 실패는 실패 응답이 아니다. `200` + 대체 답변으로 준다. (CUS-02)
- 날짜·시간은 `2026-11-11T10:00:00` (ISO-8601, 서버 시간 KST, 타임존 표기 없음)

| HTTP | 언제 |
| --- | --- |
| 200 | 조회·수정·처리 성공, 같은 요청 반복(이미 QUEUED, 이미 종료) |
| 201 | 생성 성공 (회원가입, 상담 시작) |
| 400 | 입력값 오류 |
| 401 | 로그인 안 함, 토큰 만료 |
| 403 | 역할이 맞지 않음, 남의 상담, 이용정지 |
| 404 | 없는 데이터 |
| 409 | 상태 충돌 (이미 수락됨, 종료된 상담, 진행 중 상담 있음) |
| 500 | 서버 오류 |

### 인증

- REST: `Authorization: Bearer {accessToken}`
- WebSocket: 아래 [WebSocket](#websocket) 참고 (D-10)

### 페이징

목록은 최신순, 한 페이지 20건이다. 요청은 `?page=0&size=20` (page는 0부터).

```json
"data": { "content": [ ], "page": 0, "size": 20, "totalElements": 4, "totalPages": 1 }
```

## errorCode

D-06 제안 목록이다. 도메인 공통 코드를 먼저 쓰고, 상담 관련은 `CHAT_` 접두사를 쓴다.

| errorCode | HTTP | 메시지 | 대체되는 이름 |
| --- | --- | --- | --- |
| UNAUTHORIZED | 401 | 로그인이 필요합니다. | AUTH_REQUIRED |
| ACCESS_DENIED | 403 | 접근 권한이 없습니다. | FORBIDDEN_ROLE |
| INVALID_INPUT | 400 | 입력 값을 확인해주세요. | INVALID_PARAMETER |
| INVALID_PERIOD | 400 | 조회 기간을 다시 선택해주세요. | |
| LOGIN_FAILED | 401 | 아이디 또는 비밀번호가 올바르지 않습니다. | (auth.md에 코드 없음) |
| DUPLICATE_LOGIN_ID | 409 | 이미 사용 중인 아이디입니다. | (auth.md에 코드 없음) |
| CUSTOMER_SANCTIONED | 403 | 상담 중 폭언으로 이용이 정지되었습니다. {releaseAt}에 해제됩니다. | |
| CHAT_NOT_FOUND | 404 | 상담을 찾을 수 없습니다. | CONSULTATION_NOT_FOUND |
| CHAT_ACCESS_DENIED | 403 | 접근 권한이 없습니다. (본인 상담·수락한 상담이 아님) | NOT_ASSIGNED_AGENT |
| CHAT_ALREADY_OPEN | 409 | 진행 중인 상담이 존재합니다. | |
| CHAT_NOT_IN_BOT | 409 | 상담원 연결 중에는 AI가 답하지 않습니다. | |
| CHAT_ALREADY_ACTIVE | 409 | 이미 상담원과 연결된 상담입니다. | |
| CHAT_ALREADY_ACCEPTED | 409 | 이미 다른 상담원이 수락한 상담입니다. | (chat.md에 코드 없음) |
| AGENT_BUSY | 409 | 진행 중인 상담을 먼저 종료해주세요. | (chat.md에 코드 없음) |
| AGENT_RECOVERING | 409 | 휴식 시간입니다. 잠시 후 수락해주세요. | (chat.md에 코드 없음) |
| CHAT_NOT_ACTIVE | 409 | 상담원과 상담 중인 상담이 아닙니다. | |
| CHAT_CLOSED | 409 | 종료된 상담입니다. | CHAT_ALREADY_CLOSED |
| INVALID_POLICY_VALUE | 400 | 경고 발송 기준과 자동 종료 기준은 1~5회, 정지 기간은 1~365일로 입력해주세요. | (policy API에 코드 없음) |

`INVALID_REASON`, `SANCTION_POLICY_NOT_FOUND`, `WARNING_SAVE_FAILED`처럼 한 API에서만 쓰는 코드는 각 API 문서에 둔다.

## Enum

### 회원

| enum | 값 |
| --- | --- |
| Role | `CUSTOMER`, `AGENT`, `ADMIN` (계정당 1개) |
| MemberStatus | `ACTIVE`, `INACTIVE` (INACTIVE는 로그인 불가. 이용정지는 회원 상태가 아니다) |

### 상담 상태 `ConsultationStatus` (D-01 제안값)

| 값 | 의미 | 다음 상태 | 바꾸는 API |
| --- | --- | --- | --- |
| AI_CHAT | AI 응대 중 | QUEUED, CLOSED | CUS-03 handoff, CUS-04 cancel |
| QUEUED | 상담원 대기 | IN_PROGRESS, CLOSED | API-002 accept, CUS-04 cancel |
| IN_PROGRESS | 상담원과 상담 중 | CLOSED, TERMINATED | API-006 close, MOD-03 자동 종료, MOD-07 수동 강제 종료 |
| CLOSED | 정상 종료 | 없음 | |
| TERMINATED | 폭언 종료 | 없음 | |

상태 변경은 조건부 UPDATE(`WHERE status IN (...)`)로 한다. 수락과 대기 취소처럼 동시에 들어와도 먼저 커밋된 한쪽만 성공한다.

### 상담·메시지

| enum | 값 | 쓰는 곳 |
| --- | --- | --- |
| ClosedBy | `CUSTOMER`, `AGENT`, `SYSTEM` | consultation.closed_by |
| CloseReason | `NORMAL`, `QUEUE_CANCEL`, `ABUSE_AUTO`, `ABUSE_MANUAL` (D-04) | consultation.close_reason |
| SenderType | `CUSTOMER`, `AGENT`, `BOT` (D-05) | message |
| AbuseStatus | `NORMAL`, `ABUSIVE` | message.abuse_status |
| WarningType | `AUTO`, `MANUAL` | warning.warning_type |
| SummaryResult | `RESOLVED`, `UNRESOLVED`, `TERMINATED` | consultation.result (INT-01) |

### 제재

| enum | 값 | 비고 |
| --- | --- | --- |
| SanctionStatus | `SUSPENDED`, `RELEASED`, `EXPIRED` | 저장하지 않고 계산한다. 수동 해제됨 → RELEASED, 아니면 현재 시각 < 해제 일시 → SUSPENDED, 그 외 EXPIRED |

### AI

| enum | 값 | 쓰는 곳 |
| --- | --- | --- |
| AnswerSource | `AI`, `FAQ_FALLBACK`, `NO_ANSWER` | CUS-02 응답 |
| HandoffReason | `CUSTOMER_REQUEST`, `NO_FAQ_MATCH`, `REPEATED_QUESTION` | CUS-02 응답 |
| AiMockMode | `NORMAL`, `DELAY`, `ERROR` | `ai.mock.mode`, CUS-05 |
| AiFailureType | `TIMEOUT`, `ERROR` | AiResult (INT-02) |

## 종료 처리

상담을 끝내는 API는 세 개다. 누가 어떤 상태에서 끝내는지에 따라 나눈다. (D-04 제안값)

| 상황 | API | 담당 | 이전 상태 | status | close_reason | closed_by | 이벤트 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 고객이 AI 상담 종료 | CUS-04 `cancel` | 심소민 | AI_CHAT | CLOSED | NORMAL | CUSTOMER | Closed |
| 고객이 대기 취소 | CUS-04 `cancel` | 심소민 | QUEUED | CLOSED | QUEUE_CANCEL | CUSTOMER | Closed |
| 상담 중 고객이 종료 | API-006 `close` | 정혜영 | IN_PROGRESS | CLOSED | NORMAL | CUSTOMER | Closed |
| 상담원 정상 종료 | API-006 `close` | 정혜영 | IN_PROGRESS | CLOSED | NORMAL | AGENT | Closed |
| 폭언 누적 자동 종료 | MOD-03 (내부) | 한수민 | IN_PROGRESS | TERMINATED | ABUSE_AUTO | SYSTEM | Closed + AbuseTerminated |
| 상담원 폭언 강제 종료 | MOD-07 `terminate` | 한수민 | IN_PROGRESS | TERMINATED | ABUSE_MANUAL | AGENT | Closed + AbuseTerminated (D-08) |

- 이미 종료된 상담에 다시 종료 요청이 오면 `200`과 현재 상태를 준다. 이벤트는 다시 발행하지 않는다.
- 폭언 종료 후 해당 상담원은 10분간 수락할 수 없다. (REQ-033, API-008)

## 내부 이벤트

| 이벤트 | 발행 | 수신 | 시점 | 하는 일 |
| --- | --- | --- | --- | --- |
| `ConsultationClosedEvent(consultationId)` | 종료 처리하는 모든 곳 (위 표) | 유능한 `ConsultationSummaryService` (INT-01) | `AFTER_COMMIT` | 요약 템플릿으로 inquiry_category·inquiry_summary·process_summary·result 저장. 실패하면 "요약 없음" |
| `AbuseConsultationTerminatedEvent` | 한수민 MOD-03, MOD-07 | 한수민 MOD-04 | 종료 커밋 후 | 위반 기록 → 누적 횟수로 이용정지 생성 (1회 7일, 2회 30일, 3회 이상 90일) |

- 이벤트는 상태 변경 트랜잭션 안에서 발행한다. 종료가 롤백되면 후속 처리도 실행되지 않는다.
- 같은 이벤트를 두 번 받아도 결과가 같아야 한다. (이미 요약이 있으면 건너뜀, 이미 위반이 있으면 건너뜀)

## WebSocket

D-10 제안값이다. chat.md API-005의 미정 12개를 이 표로 정리한다.

| 항목 | 제안 |
| --- | --- |
| 연결 | `ws://{host}/ws` (프론트 `NEXT_PUBLIC_WS_URL`), STOMP |
| 인증 | STOMP `CONNECT` 프레임 헤더에 `Authorization: Bearer {token}`. 브라우저 WebSocket은 HTTP 헤더를 못 붙이기 때문 (장건웅) |
| 보내기 | `SEND /app/chats/{chatId}/messages` `{ "content": "..." }`. 발신자는 서버가 토큰에서 정한다. 클라이언트가 보낸 senderId·abuseStatus는 받지 않는다 |
| 받기 | `SUBSCRIBE /topic/chats/{chatId}` 하나로 메시지·경고·상태 변경을 모두 받고 `type`으로 구분 |
| 구독 권한 | SUBSCRIBE 때 해당 상담의 고객 또는 수락한 상담원인지 서버에서 검사 (NFR-004) |
| 오류 | `/user/queue/errors`로 `{ "type": "ERROR", "errorCode": "CHAT_CLOSED", "message": "..." }` |
| 메시지 길이 | 공백 제외 1자 이상 500자 이하 (CUS-02와 같음) |
| 처리 순서 | 수신 → 상담 상태·권한 확인 → 금칙어 판정·마스킹(MOD-01) → 저장 → 발행 → 경고·종료 판단(MOD-02·03). 상담원에게는 원문이 절대 가지 않는다 (NFR-007) |
| 재연결 | 다시 연결되면 `GET /api/chats/{chatId}`로 상태를 보고, 진행 중이면 `GET .../messages`로 이력을 다시 불러온다. 종료됐으면 결과 화면으로 보낸다 (NFR-008) |
| 대기열 | 상담원 콘솔은 폴링으로 `GET /api/chats/queue` (WBS: 대기열 폴링) |

`/topic/chats/{chatId}`로 오는 메시지 `type`:

| type | 언제 | 보내는 곳 | 주요 필드 |
| --- | --- | --- | --- |
| MESSAGE | 채팅 메시지 | 정혜영 API-005 | messageId, senderType, content(상담원에게는 마스킹본), abuseStatus, createdAt |
| CONSULTATION_ACCEPTED | 상담원이 수락함 (고객 대기 화면 → 채팅 화면 전환) | 정혜영 API-002 | consultationId, agentName, acceptedAt |
| WARNING_ISSUED | 폭언 경고 | 한수민 MOD-02·06 | warningId, warningCount, message |
| CONSULTATION_CLOSED | 정상 종료 | 정혜영 API-006 | consultationId, status, closedBy, closedAt |
| CONSULTATION_TERMINATED | 폭언 종료 | 한수민 MOD-03·07 | consultationId, status, reason, closedAt |

`CONSULTATION_ACCEPTED`, `CONSULTATION_CLOSED`는 아직 어느 문서에도 없다. 제안으로 추가했다. 고객 대기 화면(SCR-006)은 처음 연결할 때 `QUEUED`인 상담을 구독하고, 이 이벤트를 받으면 채팅 화면으로 넘어간다.

D-03 확정 전에는 문서마다 경로가 다르니 주의한다. 위 경로는 `/api/chats` 기준이다.

## 화면 라우트

D-11 확정용 표다. 왼쪽은 화면 설계서, 오른쪽은 지금 `frontend/app`에 있는 페이지다.

| 화면 | 설계서 경로 | 실제 페이지 | 담당 |
| --- | --- | --- | --- |
| SCR-001 메인 | `/` | `/` | 장건웅 |
| SCR-002 회원가입 | `/signup` | `/signup` | 장건웅 |
| SCR-003 로그인 | `/login` | `/login` | 장건웅 |
| SCR-004 고객 메인 | `/customer` | `/customer` | 심소민 |
| SCR-005 AI 상담 | `/customer/consultations/:id/AI_CHAT` | `/customer/chat` | 심소민 |
| SCR-006 연결 대기 | `/customer/consultations/:id/waiting` | `/customer/waiting` | 심소민 |
| SCR-007 고객 실시간 채팅 | `/customer/consultations/:id/chat` | `/customer/live` | 심소민 |
| SCR-008 종료 결과 | `/customer/consultations/:id/result`, `/agent/consultations/:id/result` | `/customer/ended` (상담원용 없음) | 심소민, 정혜영 |
| SCR-009 이용정지 안내 | `/customer/suspension` | `/customer/suspended` | 심소민 |
| SCR-010 상담원 콘솔 | `/agent` | `/agent` | 정혜영 |
| SCR-011 상담원 채팅 | `/agent/consultations/:id/chat` | `/agent/chat` | 정혜영 |
| SCR-012 관리자 대시보드 | `/admin` | `/admin` | 유능한 |
| SCR-013 제재 관리 | `/admin/sanctions` | `/admin/sanctions` | 장건웅 |
| SCR-014 제재 상세 | `/admin/sanctions/:id` | `/admin/sanctions/[id]` | 장건웅 |
| SCR-015 정책 관리 | `/admin/policies` | `/admin/policy` | 장건웅 |
| SCR-016 상담 이력 목록 | `/agent/history`, `/admin/history` | 둘 다 있음 | 유능한 |
| SCR-017 상담 상세 | `/customer·agent·admin/history/:id` | `/admin/history/[id]`만 있음 | 유능한 |
| SCR-018 통계 대시보드 | `/admin/statistics` | 없음 (SCR-012에 합칠지 결정) | 유능한 |
| SCR-019 내 상담 이력 | `/customer/history` | `/customer/history` | 유능한 |

제안: 지금 있는 페이지 이름을 그대로 쓰고, 상담 번호는 쿼리로 넘긴다 (`/customer/live?chatId=12`). 새로고침해도 상담을 다시 찾을 수 있다. `/admin/policy`는 그대로 둔다.
