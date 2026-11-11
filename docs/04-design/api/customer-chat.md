# API 명세서 — 고객 상담·AI 상담

> 출처: Notion 「API 명세서」 하위 행 CUS-01 ~ CUS-05 (마지막 수정: 2026-11-11)

[← API 명세서 목차](../api-spec.md)

| API ID | Method | URL | 기능명 | 분류 | 인증 | 상태 | 담당자 | 비고 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| CUS-01 | POST | `/api/v1/chats` | 상담 시작 | 상담 | 필요 (CUSTOMER) | 작성완료 | 심소민 | 정지 고객 차단 포함 |
| CUS-02 | POST | `/api/v1/chats/{chatId}/bot-messages` | AI 상담 메시지 전송·응답 | AI 상담 | 필요 (CUSTOMER) | 작성완료 | 심소민 | AI 실패 시 FAQ 대체 |
| CUS-03 | PATCH | `/api/v1/chats/{chatId}/handoff` | 상담원 연결 요청 | AI 상담 | 필요 (CUSTOMER) | 작성완료 | 심소민 | BOT → QUEUED |
| CUS-04 | PATCH | `/api/v1/chats/{chatId}/cancel` | 고객 상담 종료 (AI 응대·대기 중) | 상담 | 필요 (CUSTOMER) | 작성완료 | 심소민 | ConsultationClosedEvent 발행 |
| CUS-05 | PATCH | `/api/v1/dev/ai-mock-mode` | AI 목업 모드 변경 | AI 상담 | 필요 (ADMIN) | 작성완료 | 심소민 | should, 시연용 |

---

## CUS-01 상담 시작

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API ID | CUS-01 |
| 기능 | 상담 시작 |
| Method | POST |
| URL | /api/v1/chats |
| 사용 화면 | SCR-004 고객 메인 (상담 시작 버튼) |
| 인증 필요 | 예 (CUSTOMER) |
| 설명 | 고객의 새 상담을 BOT 상태로 만든다. 이용정지 고객과 진행 중 상담이 있는 고객은 서버에서 막는다. (REQ-004, REQ-022) |

### 요청

Request Body 없음. customerId는 JWT에서 꺼낸다.

### 응답

201 Created

```json
{
  "success": true,
  "data": {
    "chatId": 12,
    "status": "BOT",
    "createdAt": "2026-11-11T10:00:00"
  },
  "message": "상담이 시작되었습니다."
}
```

### 처리 순서

1. JWT에서 customerId를 꺼낸다.
2. 해제 일시가 현재보다 뒤인 제재가 있으면 403을 돌려준다.
3. 상태가 BOT·QUEUED·ACTIVE인 상담이 있으면 409를 돌려준다.
4. CONSULTATION을 status=BOT으로 저장한다.

### 예외 상황

| 상황 | HTTP | errorCode | 처리 |
| --- | --- | --- | --- |
| 비로그인 | 401 | UNAUTHORIZED | 로그인 화면으로 이동 |
| 고객이 아닌 계정 | 403 | FORBIDDEN_ROLE | 접근 거부 |
| 이용정지 중 | 403 | CUSTOMER_SANCTIONED | data에 `reason`, `releaseAt`을 담는다. 프론트는 SCR-009로 이동 |
| 진행 중 상담 있음 | 409 | CHAT_ALREADY_OPEN | data에 기존 `chatId`, `status`를 담는다. 프론트는 상태에 맞는 화면(BOT→SCR-005, QUEUED→SCR-006, ACTIVE→SCR-007)으로 이동 |

정지 안내 문구 예: "상담 중 폭언으로 이용이 정지되었습니다. {releaseAt}에 해제됩니다." 기간은 고정하지 않는다(2회차면 30일).

### 설계 이유

- 정지 판단을 이 API에서 한다. 고객 메인에서 HIS-02로 미리 막아도, URL을 직접 호출하면 우회된다. (정책 5장)
- 제재 확인을 중복 상담 확인보다 먼저 한다. 정지 고객에게 "진행 중 상담이 있습니다"를 보여주면 정지 사실을 모른 채 기존 상담으로 이동한다.
- 409에 기존 chatId를 준다. chatId가 없으면 고객은 화면에서 닫을 수 없는 상담에 묶여 새 상담도 못 연다.
- 첫 인사말은 저장하지 않고 화면 고정 문구로 띄운다. 저장하면 고객이 아무 말 없이 나간 상담도 메시지 1개로 잡혀 INT-01의 요약 대상과 통계가 부풀려진다.

---

## CUS-02 AI 상담 메시지 전송·응답

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API ID | CUS-02 |
| 기능 | AI 상담 메시지 전송·응답 |
| Method | POST |
| URL | /api/v1/chats/{chatId}/bot-messages |
| 사용 화면 | SCR-005 Mock AI 상담 |
| 인증 필요 | 예 (CUSTOMER, 본인 상담만) |
| 설명 | 고객 메시지를 저장하고 AI 게이트웨이(INT-02)로 답변을 만든다. AI가 실패하거나 10초를 넘기면 FAQ 키워드 매칭 답변으로 대신한다. 처음 매칭된 FAQ의 카테고리를 CONSULTATION에 저장해 INT-01 요약에 쓴다. (REQ-005, 006, 008 / NFR-006, 010) |

### 요청

```json
{ "content": "수도요금 납부 방법이 어떻게 됩니까?" }
```

| 필드 | 타입 | 필수 | 규칙 |
| --- | --- | --- | --- |
| content | String | O | 공백 제외 1자 이상, 500자 이하 |

### 응답

200 OK

```json
{
  "success": true,
  "data": {
    "customerMessage": {
      "messageId": 201,
      "senderType": "CUSTOMER",
      "content": "수도요금 납부 방법이 어떻게 됩니까?",
      "createdAt": "2026-11-11T10:00:05"
    },
    "botMessage": {
      "messageId": 202,
      "senderType": "BOT",
      "content": "청구서, 자동납부, ...",
      "createdAt": "2026-11-11T10:00:06"
    },
    "answerSource": "AI",
    "faqId": 3,
    "handoffSuggested": false,
    "handoffReason": null
  },
  "message": "응답이 생성되었습니다."
}
```

| 필드 | 값 | 의미 |
| --- | --- | --- |
| answerSource | AI | 게이트웨이 정상 응답 |
|  | FAQ_FALLBACK | AI 실패 또는 10초 초과 → FAQ 키워드 매칭으로 대체 |
|  | NO_ANSWER | 맞는 FAQ 없음 → "정확한 답변을 위해 상담원을 연결해 드릴까요?" |
| handoffReason | CUSTOMER_REQUEST | 고객이 상담원을 요청함 |
|  | NO_FAQ_MATCH | 근거 FAQ 없음 |
|  | REPEATED_QUESTION | 같은 FAQ가 2회 연속 매칭됨 (Could) |

### 처리 순서

1. 본인 상담인지, 상태가 BOT인지 확인한다.
2. 고객 메시지를 저장한다.
3. 이관 조건을 확인한다. 메시지에 상담원 요청이 있거나 직전과 같은 FAQ가 다시 매칭되면 답변 대신 연결을 제안한다.
4. AI 게이트웨이를 호출한다. 제한 시간은 10초다.
5. 실패하거나 시간을 넘기면 FAQ 키워드 매칭으로 대신한다. 공백을 지운 질문에 포함된 키워드 글자 수 합이 가장 큰 FAQ를 고르고, 0점이면 NO_ANSWER다.
6. FAQ가 매칭됐고 CONSULTATION의 `inquiry_category`가 비어 있으면 그 FAQ의 카테고리와 `faq_id`를 저장한다.
7. 봇 메시지를 저장하고 AI 호출 기록(응답 시간, 성공 여부, 대체 답변 사용 여부)을 남긴다.

### 예외 상황

| 상황 | HTTP | errorCode | 처리 |
| --- | --- | --- | --- |
| 빈 메시지, 500자 초과 | 400 | INVALID_INPUT | 입력 요청 |
| 다른 고객의 상담 | 403 | CHAT_ACCESS_DENIED | 접근 거부 |
| 없는 상담 | 404 | CHAT_NOT_FOUND | 고객 메인으로 이동 |
| QUEUED·ACTIVE 상태 | 409 | CHAT_NOT_IN_BOT | 대기 중에는 AI가 답하지 않는다. 상태에 맞는 화면으로 이동 |
| CLOSED·TERMINATED 상태 | 409 | CHAT_CLOSED | 종료 결과 화면으로 이동 |
| AI 실패·10초 초과 | 200 | - | 에러로 보내지 않는다. answerSource=FAQ_FALLBACK 또는 NO_ANSWER |

프론트 요청 타임아웃은 15초로 둔다.

### 설계 이유

- AI 실패를 500이 아니라 200 + 대체 답변으로 준다. NFR-006과 정책 8장이 "AI 실패는 실패로 다루지 않는다"로 정했다. 500을 주면 프론트가 오류를 띄워 상담이 끊긴 것처럼 보인다.
- 고객 메시지를 AI 호출 전에 저장한다. AI가 10초를 다 쓰고 실패해도 고객이 보낸 문장은 남아야 한다.
- 이관 조건을 AI 호출보다 먼저 본다. 상담원을 원하는 고객에게 AI 답변을 한 번 더 보이면 불만이 쌓여 상담원에게 폭언으로 넘어간다. (AI Agent 회의록)
- 카테고리는 처음 매칭된 FAQ로만 저장하고 덮어쓰지 않는다. 매번 덮어쓰면 마지막 질문이 상담 주제가 된다. INT-01도 "첫 고객 메시지" 기준으로 다시 매칭하므로 기준이 같아야 한다.
- 스트리밍(SSE) 대신 완성된 답변을 주고 화면에서 한 글자씩 보인다. 목업 답변은 이미 완성된 문장이다. SSE는 기본 EventSource에 JWT 헤더를 실을 수 없어 별도 처리가 필요하다. 2차에 실제 AI로 바꿀 때 같은 URL에 `text/event-stream`을 추가한다.
- 프론트 타임아웃을 15초로 둔다. 서버가 10초에 AI를 끊고 대체 답변을 만들 시간이 필요하다.
- 반복 판정은 "같은 FAQ 2회 연속"으로 한다. 문장을 그대로 비교하면 표현만 바꾼 같은 질문을 놓친다.

---

## CUS-03 상담원 연결 요청

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API ID | CUS-03 |
| 기능 | 상담원 연결 요청 |
| Method | PATCH |
| URL | /api/v1/chats/{chatId}/handoff |
| 사용 화면 | SCR-005 Mock AI 상담 → SCR-006 연결 대기 |
| 인증 필요 | 예 (CUSTOMER, 본인 상담만) |
| 설명 | 상담을 BOT에서 QUEUED로 바꾸고 `queued_at`을 기록한다. 상담원 대기열(API-001)에 요청 시각 순으로 나타난다. `queued_at`은 INT-01의 처리 요약("상담원 연결 요청")에도 쓰인다. (REQ-007) |

### 요청

Request Body 없음.

### 응답

200 OK

```json
{
  "success": true,
  "data": {
    "chatId": 12,
    "status": "QUEUED",
    "queuedAt": "2026-11-11T10:03:00"
  },
  "message": "상담원 연결을 요청했습니다."
}
```

### 처리 순서

| 현재 상태 | 처리 |
| --- | --- |
| BOT | QUEUED로 바꾸고 `queued_at` 기록 → 200 |
| QUEUED | 바꾸지 않고 기존 `queued_at` 그대로 → 200 |
| ACTIVE | 409 |
| CLOSED·TERMINATED | 409 |

### 예외 상황

| 상황 | HTTP | errorCode | 처리 |
| --- | --- | --- | --- |
| 다른 고객의 상담 | 403 | CHAT_ACCESS_DENIED | 접근 거부 |
| 없는 상담 | 404 | CHAT_NOT_FOUND | 고객 메인으로 이동 |
| 이미 상담원과 연결됨 | 409 | CHAT_ALREADY_ACTIVE | SCR-007로 이동 |
| 종료된 상담 | 409 | CHAT_CLOSED | 종료 결과 화면으로 이동 |
| 요청 실패(네트워크) | - | - | 현재 화면에서 재시도 버튼 표시 |

### 설계 이유

- PATCH를 쓴다. 새 데이터가 아니라 기존 상담의 상태를 바꾼다. 정혜영 님의 수락(API-002)·종료(API-006)도 PATCH다.
- 이미 QUEUED면 에러 대신 200을 준다. 버튼 연타나 재시도로 두 번 오는 요청은 정상이다. 그리고 `queued_at`을 덮어쓰지 않아야 대기 순서가 뒤로 밀리지 않는다.
- 경로 이름을 `queue`가 아니라 `handoff`로 했다. 대기열 목록 `GET /api/v1/chats/queue`와 겹치지 않게 하기 위해서다.

---

## CUS-04 고객 상담 종료 (AI 응대·대기 중)

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API ID | CUS-04 |
| 기능 | 고객 상담 종료 (AI 응대·대기 중) |
| Method | PATCH |
| URL | /api/v1/chats/{chatId}/cancel |
| 사용 화면 | SCR-005 Mock AI 상담 (상담 종료 버튼), SCR-006 연결 대기 (대기 취소 버튼) |
| 인증 필요 | 예 (CUSTOMER, 본인 상담만) |
| 설명 | 상담원 연결 전 상담(BOT·QUEUED)을 고객이 끝낸다. 종료 사유를 기록하고 같은 트랜잭션에서 ConsultationClosedEvent를 발행한다. 상담원 연결 후(ACTIVE) 종료는 API-006이 맡는다. (REQ-013, INT-01 연계) |

### 요청

Request Body 없음.

### 응답

200 OK

```json
{
  "success": true,
  "data": {
    "chatId": 12,
    "status": "CLOSED",
    "closeReason": "QUEUE_CANCEL",
    "endedAt": "2026-11-11T10:05:00"
  },
  "message": "상담이 종료되었습니다."
}
```

### 처리 순서

| 현재 상태 | status | close_reason | closed_by | 이벤트 |
| --- | --- | --- | --- | --- |
| BOT | CLOSED | NORMAL | CUSTOMER | 발행 |
| QUEUED | CLOSED | QUEUE_CANCEL | CUSTOMER | 발행 |
| CLOSED·TERMINATED | 바꾸지 않음 | - | - | 발행 안 함, 현재 상태로 200 |
| ACTIVE | 바꾸지 않음 | - | - | 409 |

```java
// 상태 조건을 걸고 수정해 상담원 수락과 동시에 들어와도 한쪽만 성공한다
int updated = consultationRepository.closeIfNotConnected(
        chatId, closeReason, ClosedBy.CUSTOMER, now);  // WHERE status IN ('BOT','QUEUED')
if (updated == 0) {
    throw new ChatAlreadyActiveException();
}
publisher.publishEvent(new ConsultationClosedEvent(chatId));
```

### 예외 상황

| 상황 | HTTP | errorCode | 처리 |
| --- | --- | --- | --- |
| 다른 고객의 상담 | 403 | CHAT_ACCESS_DENIED | 접근 거부 |
| 없는 상담 | 404 | CHAT_NOT_FOUND | 고객 메인으로 이동 |
| 취소 직전에 상담원이 수락함 | 409 | CHAT_ALREADY_ACTIVE | "상담원이 연결되었습니다" 안내 후 SCR-007로 이동 |
| 이미 종료됨 | 200 | - | 이벤트는 다시 보내지 않고 종료 결과 화면으로 이동 |

### 설계 이유

- 별도 API로 둔다. INT-01이 종료 처리 담당을 심소민·정혜영·한수민으로 나눴고, QUEUE_CANCEL은 대기 화면(SCR-006)에서만 생긴다. API-006에 넣으면 상담원 권한 검사와 고객 대기 취소 규칙이 한 메서드에 섞인다.
- 상태 조건을 걸고 수정한다. 대기 취소와 상담원 수락(API-002)이 동시에 오면, 확인 후 저장하는 방식은 두 요청 모두 통과시킬 수 있다. 그러면 상담원은 이미 종료된 상담에 들어간다. 조건부 수정은 먼저 커밋된 하나만 성공시킨다. 수락 쪽 "먼저 처리된 1명만 성공" 규칙과 같다.
- 이벤트를 상태 변경 트랜잭션 안에서 발행한다. INT-01이 AFTER_COMMIT으로 받으므로 종료가 롤백되면 요약도 실행되지 않는다.
- 이미 종료된 상담에 다시 요청하면 200을 주고 이벤트는 다시 보내지 않는다. 연타로 에러를 보여줄 이유가 없고, 요약이 두 번 돌 이유도 없다.
- 경로를 `close`가 아니라 `cancel`로 했다. `/close`는 API-006이 쓰고 있다.

---

## CUS-05 AI 목업 모드 변경

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API ID | CUS-05 |
| 기능 | AI 목업 모드 변경 (should, 시연용) |
| Method | PATCH |
| URL | /api/v1/dev/ai-mock-mode |
| 사용 화면 | 없음 (시연 중 Postman 또는 관리자 화면에서 호출) |
| 인증 필요 | 예 (ADMIN) |
| 설명 | AI 목업의 동작을 정상·지연·오류 중 하나로 바꾼다. 서버를 재시작하지 않고 시나리오 3(AI 실패 시 FAQ 대체)을 시연하기 위한 API다. (REQ-006 검증) |

### 요청

```json
{ "mode": "DELAY" }
```

| mode | 동작 |
| --- | --- |
| NORMAL | FAQ 기반 답변을 0.5~1초 뒤 반환 |
| DELAY | 12초 대기 → 게이트웨이 10초 타임아웃 → FAQ 대체 |
| ERROR | 즉시 실패 → FAQ 대체 |

### 응답

200 OK

```json
{ "success": true, "data": { "mode": "DELAY" }, "message": "AI 목업 모드가 변경되었습니다." }
```

### 예외 상황

| 상황 | HTTP | errorCode | 처리 |
| --- | --- | --- | --- |
| 3개 외의 값 | 400 | INVALID_INPUT | 저장하지 않음 |
| ADMIN이 아님 | 403 | FORBIDDEN_ROLE | 접근 거부 |
| 서버 재시작 | - | - | `application.yml`의 `ai.mock.mode` 값으로 돌아간다 |

### 설계 이유

- 설정 파일만 쓰면 시연 중에 서버를 재시작해야 한다. 그러면 WebSocket 연결이 모두 끊겨 진행 중이던 다른 시연 흐름이 깨진다.
- 모드는 서버 전체에 적용되므로 ADMIN만 바꾼다.
