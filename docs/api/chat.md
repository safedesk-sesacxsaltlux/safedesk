# API 명세서 — 상담·채팅

[← API 명세서 목차](README.md)

| API ID | Method | URL | 기능명 | 분류 | 인증 | 상태 | 담당자 | 비고 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| API-001 | GET | `/api/chats/queue` | 상담 대기열 조회 | 상담 | 필요 | 수정완료 | 정혜영 |  |
| API-002 | PATCH | `/api/chats/{chatId}/accept` | 상담 수락 | 상담 | 필요 | 수정완료 | 정혜영 |  |
| API-003 | GET | `/api/chats/{chatId}` | 상담 상세 조회 | 상담 | 필요 | 수정완료 | 정혜영 |  |
| API-004 | GET | `/api/chats/{chatId}/messages` | 상담 메시지 조회 | 채팅 | 필요 | 수정완료 | 정혜영 |  |
| API-005 | WebSocket | `/app/chats/{chatId}/messages` | 실시간 메시지 전송 | 채팅 | 필요 | 수정완료 | 정혜영 |  |
| API-006 | PATCH | `/api/chats/{chatId}/close` | 상담 종료 | 상담 | 필요 | 수정완료 | 정혜영 |  |
| API-007 | GET | `/api/chats/{chatId}/ai-messages` | AI 응대 내역 조회 | 상담 | 필요 | 수정완료 | 정혜영 |  |
| API-008 | GET | `/api/agents/me/recovery` | 상담원 회복 시간 조회 | 상담원 | 필요 | 수정완료 | 정혜영 |  |

---

## API-001 상담 대기열 조회

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API ID | API-001 |
| 기능 | 상담 대기열 조회 |
| Method | GET |
| URL | /api/chats/queue |
| 사용 화면 | 상담원 콘솔 - 상담 대기열 |
| 호출 시점 | 상담원이 대기열 화면에 진입하거나 목록을 새로고침할 때 |
| 인증 필요 | 예 (AGENT) |
| 관련 요구사항 | 상담원 대기열 조회 관련 REQ 번호 확인 필요 |
| 설명 | 상담원에게 배정되지 않고 대기 중인 상담 목록을 조회한다. 상담 상태가 QUEUED인 상담을 대상으로 한다. |

### Request Header

| 이름 | 값 | 필수 | 설명 |
| --- | --- | --- | --- |
| Authorization | Bearer {accessToken} | Y | 상담원 계정 JWT |
| Content-Type | application/json | N | GET 요청으로 별도 Body 없음 |

### Request Body

없음 (GET 요청)

### Response Body

아래는 기존 `ChatQueueResponseDTO` 필드를 기준으로 작성한 응답 데이터 예시다. 공통 응답 래퍼(`success`, `message`, `data`) 사용 여부는 팀 확인이 필요하다.

```json
[
  {
    "consultationId": 101,
    "customerId": "12",
    "customerName": "김고객",
    "consultationStatus": "QUEUED",
    "requestedAt": "2026-11-11T13:00:00",
    "waitingTime": 300
  },
  {
    "consultationId": 102,
    "customerId": "15",
    "customerName": "이고객",
    "consultationStatus": "QUEUED",
    "requestedAt": "2026-11-11T13:03:00",
    "waitingTime": 120
  }
]
```

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| consultationId | Long | Y | 상담 고유 번호 |
| customerId | String | Y | 고객 식별 번호 |
| customerName | String | N | 고객 이름 (`member.name`). 상담원 대기열 화면에 회원 번호 대신 표시 |
| consultationStatus | String | Y | 상담 상태. 조회 대상은 QUEUED |
| requestedAt | LocalDateTime | Y | 상담 대기 요청 시각 |
| waitingTime | long | Y | 상담 대기 시간. 단위 확인 필요 |

### Status Code

| 상태 코드 | 설명 |
| --- | --- |
| 200 OK | 상담 대기열 조회 성공 |
| 401 Unauthorized | 인증되지 않은 사용자 |
| 403 Forbidden | 상담원 권한 없음 |
| 500 Internal Server Error | 서버 내부 오류 |

### 예외 상황

| 상황 | 응답 코드 | 메시지 |
| --- | --- | --- |
| 대기 중인 상담이 없음 | 200 OK | 빈 목록 반환 |
| JWT 누락 또는 유효하지 않음 | 401 Unauthorized | 로그인이 필요합니다. |
| 상담원 권한이 없는 사용자 | 403 Forbidden | 접근 권한이 없습니다. |
| 서버 내부 오류 | 500 Internal Server Error | 서버 오류가 발생했습니다. |

### 팀 확인 필요 사항

1. ERD의 `customer_id`는 BIGINT인데 기존 DTO의 `customerId`는 String으로 선언되어 있어 타입 통일이 필요하다.
2. DTO의 `requestedAt`이 ERD의 `queued_at`을 의미하는지 확인해야 한다.
3. `waitingTime`의 단위(초 또는 분)를 확정해야 한다.
4. 공통 응답 형식 및 관련 요구사항 번호를 확인해야 한다.
5. 대기열 정렬 기준을 확정해야 한다.

---

## API-002 상담 수락

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API ID | API-002 |
| 기능 | 상담 수락 |
| Method | PATCH |
| URL | /api/chats/{chatId}/accept |
| 사용 화면 | 상담원 콘솔 - 상담 대기열 |
| 호출 시점 | 상담원이 대기 중인 상담의 수락 버튼을 클릭할 때 |
| 인증 필요 | 예 (AGENT) |
| 관련 요구사항 | 상담 수락 및 상담원 배정 관련 REQ 번호 확인 필요 |
| 설명 | 상담원이 대기 중인 상담을 수락한다. 상담 상태를 QUEUED에서 IN_PROGRESS로 변경하고, 수락한 상담원을 해당 상담에 배정한다. 하나의 상담은 한 명의 상담원만 수락할 수 있으며, 이미 상담 중인 상담원은 다른 상담을 수락할 수 없다. |

### Request Header

| 이름 | 값 | 필수 | 설명 |
| --- | --- | --- | --- |
| Authorization | Bearer {accessToken} | Y | 상담원 계정 JWT |
| Content-Type | application/json | N | 별도 Request Body 없음 |

### Path Variable

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| chatId | Long | Y | 수락할 상담의 고유 번호 (`consultation_id`) |

### Request Body

없음.

상담원 식별 정보는 JWT 인증 정보를 통해 확인하는 방식으로 작성했다. 실제 JWT에서 `member_id`를 추출하는 방법은 인증 담당자와 확인이 필요하다.

### Response Body

아래는 응답 구조 예시이며, 실제 공통 응답 형식은 팀 확인이 필요하다.

```json
{
  "success": true,
  "message": "상담이 수락되었습니다.",
  "data": {
    "consultationId": 101,
    "customerId": 12,
    "agentId": 5,
    "consultationStatus": "IN_PROGRESS",
    "acceptedAt": "2026-11-11T13:10:00"
  }
}
```

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| success | Boolean | Y | 요청 처리 성공 여부 |
| message | String | Y | 처리 결과 메시지 |
| data | Object | Y | 수락된 상담 정보 |
| consultationId | Long | Y | 상담 고유 번호 |
| customerId | Long | Y | 고객 회원 번호 |
| agentId | Long | Y | 상담을 수락한 상담원 회원 번호 |
| consultationStatus | String | Y | 변경된 상담 상태 (`IN_PROGRESS`) |
| acceptedAt | LocalDateTime | Y | 상담 수락 시각 |

### Status Code

| 상태 코드 | 설명 |
| --- | --- |
| 200 OK | 상담 수락 성공 |
| 400 Bad Request | 잘못된 상담 번호 또는 요청 |
| 401 Unauthorized | 인증되지 않은 사용자 |
| 403 Forbidden | 상담원 권한 없음 |
| 404 Not Found | 해당 상담이 존재하지 않음 |
| 409 Conflict | 이미 수락된 상담이거나 상담원이 다른 상담을 진행 중임 |
| 500 Internal Server Error | 서버 내부 오류 |

### 예외 상황

| 상황 | 응답 코드 | 메시지 |
| --- | --- | --- |
| 상담 번호 형식이 올바르지 않음 | 400 Bad Request | 상담 번호를 확인해주세요. |
| JWT 누락 또는 유효하지 않음 | 401 Unauthorized | 로그인이 필요합니다. |
| 상담원 권한이 없는 사용자 | 403 Forbidden | 접근 권한이 없습니다. |
| 상담 번호에 해당하는 상담이 없음 | 404 Not Found | 상담을 찾을 수 없습니다. |
| 다른 상담원이 먼저 수락함 | 409 Conflict | 이미 다른 상담원이 수락한 상담입니다. |
| 상담원이 이미 다른 상담을 진행 중임 | 409 Conflict | 진행 중인 상담을 종료한 후 수락할 수 있습니다. |
| 상담 상태가 QUEUED가 아님 | 409 Conflict | 현재 수락할 수 없는 상담입니다. |
| 상담원이 회복시간 중임 | 409 Conflict | 회복시간이 종료된 후 상담을 수락할 수 있습니다. |
| 서버 내부 오류 | 500 Internal Server Error | 서버 오류가 발생했습니다. |

### 처리 규칙

1. 상담 상태가 `QUEUED`인 경우에만 수락할 수 있다.
2. 상담원은 동시에 하나의 상담만 진행할 수 있다.
3. 여러 상담원이 같은 상담을 수락하려는 경우 최초로 수락에 성공한 상담원만 배정된다.
4. 수락 성공 시 `consultation.agent_id`에 상담원 회원 번호를 저장한다.
5. 상담 상태를 `IN_PROGRESS`로 변경한다.
6. `consultation.accepted_at`에 수락 시각을 기록한다.
7. 상담원이 회복시간 중이면 새로운 상담을 수락할 수 없다.

### 팀 확인 필요 사항

1. JWT 인증 정보에서 상담원 `member_id`를 가져오는 방식 확인.
2. 동시 수락 방지를 위한 DB 잠금 또는 조건부 UPDATE 처리 방식 확정.
3. 공통 Response Body 형식 확인.
4. 상담원 회복시간 확인을 위한 데이터 조회 방식 확정.
5. 관련 요구사항 번호 확인.
6. 상담 수락 성공 후 고객과 상담원 화면에 상태 변경을 알리는 방식 확인.

---

## API-003 상담 상세 조회

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API ID | API-003 |
| 기능 | 상담 상세 조회 |
| Method | GET |
| URL | /api/chats/{chatId} |
| 사용 화면 | 상담원 실시간 상담 화면, 고객 채팅 화면(재연결 시 상태 확인) |
| 호출 시점 | 상담 화면에 들어올 때, WebSocket이 다시 연결될 때 (contract.md WebSocket 「재연결」) |
| 인증 필요 | 예 (해당 상담의 CUSTOMER 또는 수락한 AGENT) |
| 관련 요구사항 | REQ-019 (고객 누적 위반 표시), NFR-004 (상담 접근 제한), NFR-008 (재연결 시 상태 확인) |
| 설명 | 상담의 상태, 고객·상담원 정보, 종료 정보, 고객 누적 위반 수, 이번 상담 경고 수와 자동 종료 기준을 조회한다. 그 상담의 고객이나 수락한 상담원만 조회할 수 있다. 상담 메시지 내역은 API-004에서 별도로 조회한다. |

### Request Header

| 이름 | 값 | 필수 | 설명 |
| --- | --- | --- | --- |
| Authorization | Bearer {accessToken} | Y | 고객 또는 상담원 계정 JWT |
| Content-Type | application/json | N | GET 요청으로 별도 Body 없음 |

### Path Variable

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| chatId | Long | Y | 조회할 상담 고유 번호 (`consultation_id`) |

### Request Body

없음 (GET 요청)

### Response Body

구현(`ChatDetailResponseDTO`) 기준이다.

```json
{
  "success": true,
  "message": "조회되었습니다.",
  "data": {
    "consultationId": 101,
    "customerId": 12,
    "customerName": "김고객",
    "agentId": 5,
    "agentName": "이상담",
    "consultationStatus": "IN_PROGRESS",
    "inquiryCategory": "배송",
    "inquirySummary": "배송 지연 문의",
    "createdAt": "2026-11-11T12:55:00",
    "queuedAt": "2026-11-11T13:00:00",
    "acceptedAt": "2026-11-11T13:10:00",
    "closedAt": null,
    "closedBy": null,
    "closeReason": null,
    "customerViolationCount": 1,
    "warningCount": 0,
    "closeThreshold": 2
  }
}
```

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| success | Boolean | Y | 요청 처리 성공 여부 |
| message | String | Y | 처리 결과 메시지 |
| data | Object | Y | 상담 상세 정보 |
| consultationId | Long | Y | 상담 고유 번호 |
| customerId | Long | Y | 고객 회원 번호 |
| customerName | String | N | 고객 이름 (`member.name`) |
| agentId | Long | 조건부 | 수락한 상담원 회원 번호. 수락 전이면 NULL |
| agentName | String | N | 수락한 상담원 이름. 수락 전이면 NULL |
| consultationStatus | String | Y | 상담 상태 (`AI_CHAT`, `QUEUED`, `IN_PROGRESS`, `CLOSED`, `TERMINATED`) |
| inquiryCategory | String | N | 상담 문의 유형 |
| inquirySummary | String | N | 상담 문의 요약 |
| createdAt | LocalDateTime | Y | 상담 생성 시각 |
| queuedAt | LocalDateTime | N | 상담 대기열 진입 시각 |
| acceptedAt | LocalDateTime | N | 상담 수락 시각 |
| closedAt | LocalDateTime | N | 상담 종료 시각. 진행 중이면 NULL |
| closedBy | String | N | 종료 주체 (`CUSTOMER`, `AGENT`, `SYSTEM`). 진행 중이면 NULL |
| closeReason | String | N | 종료 사유 (`NORMAL`, `ABUSE_AUTO`, `ABUSE_MANUAL`). 진행 중이면 NULL |
| customerViolationCount | Long | Y | 고객 누적 위반 수 (REQ-019) |
| warningCount | Long | Y | 이번 상담의 경고 수 |
| closeThreshold | Integer | N | 이번 상담에 적용된 정책의 자동 종료 기준 경고 수 |

### Status Code

| 상태 코드 | 설명 |
| --- | --- |
| 200 OK | 상담 상세 조회 성공 |
| 400 Bad Request | 상담 번호 형식 오류 |
| 401 Unauthorized | 인증되지 않은 사용자 (`UNAUTHORIZED`) |
| 403 Forbidden | 해당 상담의 고객·수락한 상담원이 아님 (`CHAT_ACCESS_DENIED`) |
| 404 Not Found | 해당 상담이 존재하지 않음 (`CHAT_NOT_FOUND`) |
| 500 Internal Server Error | 서버 내부 오류 |

### 예외 상황

| 상황 | 응답 코드 | errorCode |
| --- | --- | --- |
| 상담 번호 형식이 올바르지 않음 | 400 Bad Request | `INVALID_INPUT` |
| JWT 누락 또는 유효하지 않음 | 401 Unauthorized | `UNAUTHORIZED` |
| 해당 상담의 고객도, 수락한 상담원도 아님 (다른 상담원·다른 고객) | 403 Forbidden | `CHAT_ACCESS_DENIED` |
| 상담 번호에 해당하는 상담이 없음 | 404 Not Found | `CHAT_NOT_FOUND` |
| 서버 내부 오류 | 500 Internal Server Error | `INTERNAL_SERVER_ERROR` |

### 처리 규칙

1. JWT로 로그인 여부를 확인한다. 역할(CUSTOMER·AGENT)은 따로 막지 않고, 상담 참여자인지로 판단한다.
2. `chatId`에 해당하는 `consultation` 데이터를 조회한다.
3. 요청자가 그 상담의 고객(`customer_id`)이거나 수락한 상담원(`agent_id`)일 때만 응답한다 (NFR-004).
4. 상태와 상관없이 조회할 수 있다 (종료된 상담도 결과 확인·재연결 판단에 쓴다).
5. `warningCount`·`closeThreshold`는 상담에 저장된 정책 기준이라 MOD-05 경고 내역의 값과 같다.
6. 상담 메시지 목록은 이 API에서 반환하지 않고 API-004를 통해 조회한다.
7. 조회 요청은 상담 상태나 상담 내용을 변경하지 않는다.

### 팀 확인 필요 사항

1. 종료된 상담(`CLOSED`, `TERMINATED`)도 이 API로 조회할 수 있는지 확인.
2. 상담 수락 이전(`QUEUED`)에 상세 조회를 허용할지 확인.
3. 고객 이름 등 `member` 테이블의 추가 정보를 응답에 포함할지 확인.
4. `inquiryCategory`, `inquirySummary`가 NULL인 경우 화면 표시 방식 확인.
5. 공통 Response Body 형식 확인.
6. 관련 요구사항 번호 확인.
7. 상담 상세 조회에 사용할 DTO 필드 구성 확정.

---

## API-004 상담 메시지 조회

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API ID | API-004 |
| 기능 | 상담 메시지 내역 조회 |
| Method | GET |
| URL | /api/chats/{chatId}/messages |
| 사용 화면 | 상담원 실시간 상담 화면, 고객 실시간 채팅 화면 |
| 호출 시점 | 채팅 화면에 들어올 때, WebSocket이 다시 연결될 때 (끊긴 동안의 메시지 복구) |
| 인증 필요 | 예 (해당 상담의 CUSTOMER 또는 수락한 AGENT) |
| 관련 요구사항 | NFR-004 (상담 접근 제한), NFR-005 (원문 비공개), NFR-008 (재연결 시 이력 복구) |
| 설명 | 특정 상담에 저장된 메시지 내역을 조회한다. 그 상담의 고객이나 수락한 상담원만 조회할 수 있으며, 폭언으로 판정된 메시지는 마스킹된 내용을 반환한다. 실시간 메시지 송수신은 API-005 WebSocket에서 처리한다. |

### Request Header

| 이름 | 값 | 필수 | 설명 |
| --- | --- | --- | --- |
| Authorization | Bearer {accessToken} | Y | 고객 또는 상담원 계정 JWT |
| Content-Type | application/json | N | GET 요청으로 별도 Body 없음 |

### Path Variable

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| chatId | Long | Y | 메시지를 조회할 상담 고유 번호 (`consultation_id`) |

### Request Body

없음 (GET 요청)

### Response Body

구현(`ChatHistoryResponseDTO`) 기준이다. AI 상담 단계의 고객·AI 메시지도 함께 온다 (`senderType`으로 구분).

```json
{
  "success": true,
  "message": "조회되었습니다.",
  "data": [
    {
      "messageId": 1001,
      "consultationId": 101,
      "senderId": 12,
      "senderType": "CUSTOMER",
      "content": "배송이 언제 도착하나요?",
      "abuseStatus": "NORMAL",
      "createdAt": "2026-11-11T13:10:00"
    },
    {
      "messageId": 1002,
      "consultationId": 101,
      "senderId": 5,
      "senderType": "AGENT",
      "content": "안녕하세요. 배송 상태를 확인해 드리겠습니다.",
      "abuseStatus": "NORMAL",
      "createdAt": "2026-11-11T13:10:30"
    },
    {
      "messageId": 1003,
      "consultationId": 101,
      "senderId": 12,
      "senderType": "CUSTOMER",
      "content": "** 같은 답변이네요.",
      "abuseStatus": "ABUSIVE",
      "createdAt": "2026-11-11T13:11:00"
    }
  ]
}
```

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| success | Boolean | Y | 요청 처리 성공 여부 |
| message | String | Y | 처리 결과 메시지 |
| data | Array | Y | 상담 메시지 목록 |
| messageId | Long | Y | 메시지 고유 번호 |
| consultationId | Long | Y | 상담 고유 번호 |
| senderId | Long | N | 발신자 회원 번호. AI·시스템 메시지는 NULL |
| senderType | String | Y | 발신자 유형 (`CUSTOMER`, `AGENT`, `AI`, `SYSTEM`) |
| content | String | Y | 메시지 내용. 폭언인 경우 마스킹된 내용 |
| abuseStatus | String | N | 폭언 판정 결과 (`NORMAL`, `ABUSIVE`) |
| createdAt | LocalDateTime | Y | 메시지 생성 시각 |

### Status Code

| 상태 코드 | 설명 |
| --- | --- |
| 200 OK | 상담 메시지 내역 조회 성공 |
| 400 Bad Request | 상담 번호 형식 오류 |
| 401 Unauthorized | 인증되지 않은 사용자 (`UNAUTHORIZED`) |
| 403 Forbidden | 해당 상담의 고객·수락한 상담원이 아님 (`CHAT_ACCESS_DENIED`) |
| 404 Not Found | 해당 상담이 존재하지 않음 (`CHAT_NOT_FOUND`) |
| 500 Internal Server Error | 서버 내부 오류 |

### 예외 상황

| 상황 | 응답 코드 | errorCode |
| --- | --- | --- |
| 상담 번호 형식이 올바르지 않음 | 400 Bad Request | `INVALID_INPUT` |
| JWT 누락 또는 유효하지 않음 | 401 Unauthorized | `UNAUTHORIZED` |
| 해당 상담의 고객도, 수락한 상담원도 아님 (다른 상담원·다른 고객) | 403 Forbidden | `CHAT_ACCESS_DENIED` |
| 상담 번호에 해당하는 상담이 없음 | 404 Not Found | `CHAT_NOT_FOUND` |
| 조회할 메시지가 없음 | 200 OK | 빈 배열 반환 |
| 서버 내부 오류 | 500 Internal Server Error | `INTERNAL_SERVER_ERROR` |

### 처리 규칙

1. JWT로 로그인 여부를 확인한다. 역할(CUSTOMER·AGENT)은 따로 막지 않고, 상담 참여자인지로 판단한다.
2. `chatId`에 해당하는 상담이 존재하는지 확인한다.
3. 요청자가 그 상담의 고객이거나 수락한 상담원일 때만 응답한다 (NFR-004).
4. `message.consultation_id`가 요청한 `chatId`와 일치하는 메시지를 조회한다.
5. 메시지는 `created_at` 오름차순, 같은 시각이면 `message_id` 오름차순으로 반환한다 (PR #66 리뷰 반영).
6. `abuse_status`가 `ABUSIVE`인 메시지는 마스킹된 `message.content`를 반환한다.
7. 폭언 원문인 `abuse_log.original_content`는 일반 상담원 메시지 조회 응답에 포함하지 않는다.
8. 메시지 조회는 상담 상태나 메시지 데이터를 변경하지 않는다.

### 팀 확인 필요 사항

1. `sender_type`에 사용할 정확한 값(`CUSTOMER`, `AGENT`, `AI` 등) 확인.
2. AI 1차 응대 메시지가 `message` 테이블에 함께 저장되는지 확인.
3. 메시지 정렬 기준을 `created_at` 오름차순으로 확정할지 확인.
4. 메시지 개수가 많을 경우 페이지네이션 적용 여부 확인.
5. 상담 종료 후(`CLOSED`, `TERMINATED`) 메시지 내역 조회 허용 여부 확인.
6. `abuse_status`가 NULL인 메시지의 처리 방식 확인.
7. 공통 Response Body 형식 및 관련 요구사항 번호 확인.
8. 메시지 저장 담당자(심소민) 및 폭언 감지 담당자(한수민)와 조회 데이터 연동 방식 확인.

---

## API-005 실시간 메시지 전송

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API ID | API-005 |
| 기능 | 실시간 상담 메시지 전송 |
| Method | WebSocket (STOMP SEND) |
| WebSocket 연결 URL | /ws |
| 메시지 전송 Destination | /app/chats/{chatId}/messages |
| 사용 화면 | 고객 상담 채팅 화면 / 상담원 콘솔 채팅 화면 |
| 호출 시점 | 고객 또는 상담원이 메시지 전송 버튼을 클릭하거나 Enter 키로 메시지를 전송할 때 |
| 인증 필요 | 예 (CUSTOMER / AGENT) |
| 관련 요구사항 | 실시간 상담 메시지 송수신 관련 REQ 번호 확인 필요 |
| 설명 | 고객과 상담원이 WebSocket을 통해 실시간으로 메시지를 주고받는다. 서버는 메시지의 상담 정보와 발신자 권한을 확인하고, 폭언 감지 및 메시지 저장 기능과 연동한 뒤 상대방에게 메시지를 전달한다. |

### Request Header

WebSocket 연결 및 STOMP 메시지 전송 기준이다.

| 이름 | 값 | 필수 | 설명 |
| --- | --- | --- | --- |
| Authorization | Bearer {accessToken} | 확인 필요 | JWT 인증 정보. WebSocket 연결 또는 STOMP CONNECT 단계에서 전달 방식 확정 필요 |
| destination | /app/chats/{chatId}/messages | Y | STOMP SEND 대상 |
| content-type | application/json | Y | 메시지 Payload 형식 |

※ 브라우저 기본 WebSocket API에서는 HTTP 연결 요청에 임의의 Authorization 헤더를 직접 설정할 수 없다. JWT 인증 담당자와 STOMP CONNECT 헤더 또는 다른 인증 방식의 사용 여부를 확정해야 한다.

### Path Variable

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| chatId | Long | Y | 메시지를 전송할 상담 고유 번호 (`consultation_id`) |

※ REST URL의 Path Variable이 아니라 STOMP destination 경로에 포함되는 값이다.

### Request Body

기존 `ChatMessageRequestDTO`에 정의된 필드를 기준으로 작성한 Payload 예시다.

```json
{
  "consultationId": 101,
  "senderType": "CUSTOMER",
  "senderMemberId": 12,
  "messageContent": "배송이 언제 도착하나요?",
  "abuseStatus": null
}
```

| 필드 | 타입 | 필수 | 검증 규칙 및 설명 |
| --- | --- | --- | --- |
| consultationId | Long | Y | 메시지가 속한 상담 번호. destination의 chatId와 일치해야 함 |
| senderType | String | Y | 발신자 유형. 허용 값은 팀 확인 필요 |
| senderMemberId | Long | 확인 필요 | 발신자 회원 번호. 실제 신원은 JWT 기준으로 검증해야 함 |
| messageContent | String | Y | 메시지 내용. 빈 문자열 및 최대 길이 제한 확인 필요 |
| abuseStatus | String | N | 폭언 판정 결과. 클라이언트 입력값을 신뢰하지 않으며 서버에서 판정 |

※ 현재 DTO에 `abuseStatus` 필드가 존재하지만, 폭언 판정은 한수민 담당 기능에서 처리한다. 클라이언트가 임의로 `NORMAL`을 지정해 폭언 감지를 우회할 수 없도록 해야 한다.

### Response Body

WebSocket 메시지 전송은 REST API처럼 HTTP 응답 본문을 반환하지 않는다.

대신 서버가 STOMP 구독 경로로 메시지를 발행하면 해당 경로를 구독 중인 클라이언트가 수신한다.

**메시지 수신 Payload 예시 (제안)**

```json
{
  "messageId": 1001,
  "consultationId": 101,
  "senderId": 12,
  "senderType": "CUSTOMER",
  "content": "배송이 언제 도착하나요?",
  "abuseStatus": "NORMAL",
  "createdAt": "2026-11-11T13:15:00"
}
```

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| messageId | Long | Y | 저장된 메시지 고유 번호 |
| consultationId | Long | Y | 상담 고유 번호 |
| senderId | Long | Y | 실제 발신자 회원 번호 |
| senderType | String | Y | 고객 또는 상담원 등 발신자 유형 |
| content | String | Y | 메시지 내용. 폭언이면 마스킹된 내용 |
| abuseStatus | String | N | 폭언 판정 결과 (`NORMAL`, `ABUSIVE`) |
| createdAt | LocalDateTime | Y | 메시지 생성 시각 |

**STOMP 구독 Destination (확인 필요)**

```text
/topic/chats/{chatId}/messages
```

현재 `WebSocketConfig`의 `/topic` 브로커 접두사를 기준으로 제안한 경로다. 실제 메시지 발행 Destination과 구독 권한은 팀에서 확정해야 한다.

### Status Code

WebSocket/STOMP 메시지 처리에는 REST API의 HTTP 상태 코드를 그대로 적용하지 않는다.

| 상태 또는 오류 | 설명 |
| --- | --- |
| HTTP 101 Switching Protocols | WebSocket 핸드셰이크 성공 시 일반적으로 사용 |
| STOMP CONNECTED | STOMP 연결 성공 |
| STOMP ERROR | STOMP 프로토콜 또는 서버 처리 오류 |
| 인증 실패 | JWT 누락·만료·유효하지 않은 인증 정보 |
| 권한 없음 | 해당 상담에 메시지를 보낼 권한이 없음 |
| 상담 상태 오류 | 실시간 상담이 허용되지 않는 상태 |
| 메시지 검증 실패 | 필수 필드 누락 또는 메시지 내용 오류 |
| 연결 종료 | 네트워크 오류 또는 서버 측 연결 종료 |

※ 구체적인 오류 전달 방식(STOMP ERROR 프레임, 별도 오류 Destination, 연결 종료 등)은 미확정이다.

### 예외 상황

| 상황 | 처리 방식 | 메시지 예시 |
| --- | --- | --- |
| JWT 누락 또는 유효하지 않음 | 연결 또는 메시지 처리 거부 | 로그인이 필요합니다. |
| 해당 상담이 존재하지 않음 | 메시지 전송·구독 거부 (`CHAT_ACCESS_DENIED`, 남의 상담과 같은 응답) | 접근 권한이 없습니다. |
| 상담 참여자가 아닌 사용자가 전송 | 메시지 전송 거부 | 해당 상담에 접근할 권한이 없습니다. |
| 다른 상담원에게 배정된 상담에 전송 | 메시지 전송 거부 | 해당 상담에 접근할 권한이 없습니다. |
| 메시지 내용이 비어 있음 | 메시지 전송 거부 | 메시지 내용을 입력해주세요. |
| 상담이 CLOSED 상태 | 메시지 전송 거부 | 종료된 상담입니다. |
| 상담이 TERMINATED 상태 | 메시지 전송 거부 | 강제 종료된 상담입니다. |
| 폭언이 감지됨 | 마스킹 및 폭언 처리 기능 연동 | 폭언 감지 결과에 따라 처리 |
| WebSocket 연결 끊김 | 클라이언트 연결 상태 갱신 | 연결이 끊어졌습니다. |

※ 위 오류 메시지는 예시이며 실제 오류 프레임과 응답 구조는 팀에서 확정해야 한다.

### 처리 규칙

1. 클라이언트는 `/ws` 엔드포인트로 WebSocket 연결을 시도한다.
2. STOMP 연결 및 사용자 인증을 완료한다.
3. 클라이언트는 `/app/chats/{chatId}/messages`로 메시지를 전송한다.
4. 서버는 JWT를 통해 실제 발신자를 식별하고 상담 참여 권한을 확인한다.
5. 상담원 메시지인 경우 해당 상담을 수락한 상담원인지 확인한다.
6. 고객과 상담원 간 실시간 상담 메시지는 원칙적으로 `IN_PROGRESS` 상태에서 송수신한다. AI_CHAT 단계의 고객 메시지는 심소민 담당 기능과 구분한다.
7. 고객 메시지는 한수민 담당 폭언 감지 기능과 연동한다.
8. 정상 메시지는 원문을 저장하고, 폭언 메시지는 마스킹된 내용을 `message.content`에 저장한다.
9. 폭언 원문은 `abuse_log.original_content`에 별도 저장하며, 일반 상담원 채팅 메시지로 노출하지 않는다.
10. `message.abuse_status`에는 서버가 판정한 `NORMAL` 또는 `ABUSIVE` 값을 기록한다.
11. 메시지 저장 기능과 연동한 뒤, 실제 전송 가능한 메시지를 해당 상담의 구독자에게 발행한다.
12. 상담이 `CLOSED` 또는 `TERMINATED` 상태가 되면 추가 상담 메시지 전송을 차단한다.
13. 폭언 누적에 따른 경고, 자동 종료 및 제재는 한수민 담당 기능과 연동한다.
14. SEND는 `/app/chats/{chatId}/messages`로만 받는다. `/topic/**`, `/user/**`, `/queue/**` 등 브로커로 바로 가는 목적지로 보내면 거부하고 보낸 사람의 `/user/queue/errors`로 `ACCESS_DENIED`를 알린다 (가짜 메시지·가짜 `CONSULTATION_CLOSED` 방지, PR #80 리뷰).
15. SEND 목적지가 맞아도 그 상담의 고객이나 수락한 상담원이 아니면 거부한다 (`CHAT_ACCESS_DENIED`). SUBSCRIBE도 같다. 없는 상담도 남의 상담과 똑같이 `CHAT_ACCESS_DENIED`로 거부해서, 응답으로 상담 번호가 있는지 알 수 없게 한다 (PR #80 리뷰). REST(API-003·004·006)는 계약대로 없는 상담이면 404 `CHAT_NOT_FOUND`를 유지한다.
16. CONNECT 때 검증한 토큰과 만료 시각을 세션에 저장하고, SEND·SUBSCRIBE 때마다 로그아웃(블랙리스트)·만료를 다시 확인한다. 실패하면 `/user/queue/errors`로 `UNAUTHORIZED`를 알리고 그 프레임을 버린다. 클라이언트는 토큰을 재발급해 다시 연결한다.
17. 본문이 JSON이 아니거나 형식이 맞지 않으면 `/user/queue/errors`로 `INVALID_INPUT`을 알린다.

### 팀 확인 필요 사항

1. **JWT 인증 방식:** WebSocket/STOMP 연결 시 인증 정보를 전달하고 검증하는 방식. 장건웅 담당.
2. **메시지 저장 주체:** 심소민 담당 메시지 저장 기능과 상담원 WebSocket 메시지 처리 기능의 호출 관계.
3. **폭언 감지 연동:** 한수민 담당 기능을 어느 단계에서 호출할지, 마스킹 완료 메시지를 어떻게 전달받을지.
4. **STOMP 구독 Destination:** `/topic/chats/{chatId}/messages` 사용 여부 및 실제 발행 경로.
5. **구독 권한:** 상담 참여자 외 사용자가 메시지를 구독하지 못하도록 검증하는 방식.
6. **DTO 구조:** 기존 `ChatMessageRequestDTO`의 `senderMemberId`, `abuseStatus`를 클라이언트 요청에 포함할지 여부.
7. **상담 번호:** destination의 `chatId`와 Payload의 `consultationId` 중 어떤 값을 기준으로 처리할지.
8. **메시지 길이:** 최대 글자 수 및 공백 메시지 처리 규칙.
9. **오류 전달:** STOMP ERROR 또는 별도 오류 메시지 방식.
10. **강제 종료 알림:** 폭언 담당 기능에서 `TERMINATED`로 상태를 변경할 때 상담원 화면에 알리는 방식.
11. **WebSocket 설정:** 현재 `@MessageMapping`의 경로 변수 처리, CORS 설정, 메시지 발행 구현 여부 확인.
12. **관련 요구사항 번호:** 요구사항 정의서와 최종 매핑 필요.

---

## API-006 상담 종료

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API ID | API-006 |
| 기능 | 상담 종료 |
| Method | PATCH |
| URL | /api/chats/{chatId}/close |
| 사용 화면 | 상담원 실시간 상담 화면, 고객 실시간 채팅 화면 |
| 호출 시점 | 상담원 또는 고객이 상담 종료 버튼을 클릭할 때 |
| 인증 필요 | 예 (수락한 AGENT 또는 해당 상담의 CUSTOMER) |
| 관련 요구사항 | NFR-004 (상담 접근 제한), 체크리스트 E7 (종료된 상담 전송 차단) |
| 설명 | 진행 중인 상담을 일반 종료(`CLOSED`)하고 종료 정보를 기록한다. 폭언 자동 종료(`TERMINATED`)는 이 API가 아니라 MOD-03 내부 처리에서 한다. |

### Request Header

| 이름 | 값 | 필수 | 설명 |
| --- | --- | --- | --- |
| Authorization | Bearer {accessToken} | Y | 수락한 상담원 또는 해당 상담 고객의 JWT |

### Path Variable

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| chatId | Long | Y | 종료할 상담 고유 번호 (`consultation_id`) |

### Request Body

없음. 종료 주체(`closedBy`)와 사유(`closeReason`)는 서버가 정한다.

- `closedBy`: 요청자가 수락한 상담원이면 `AGENT`, 상담 고객이면 `CUSTOMER`
- `closeReason`: 이 API로 종료하면 항상 `NORMAL`

### Response Body

구현(`ChatCloseResponseDTO`) 기준이다.

```json
{
  "success": true,
  "message": "상담이 종료되었습니다.",
  "data": {
    "consultationId": 101,
    "consultationStatus": "CLOSED",
    "agentId": 5,
    "closedAt": "2026-11-11T14:00:00",
    "closedBy": "AGENT",
    "closeReason": "NORMAL"
  }
}
```

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| success | Boolean | Y | 요청 처리 성공 여부 |
| message | String | Y | 처리 결과 메시지 |
| data | Object | Y | 종료된 상담 정보 |
| consultationId | Long | Y | 상담 고유 번호 |
| consultationStatus | String | Y | 종료 후 상담 상태 (`CLOSED` 또는 `TERMINATED`) |
| agentId | Long | N | 배정된 상담원 회원 번호 |
| closedAt | LocalDateTime | Y | 상담 종료 시각 |
| closedBy | String | N | 상담 종료 주체 (`AGENT`, `CUSTOMER`, `SYSTEM`) |
| closeReason | String | N | 상담 종료 사유 (`NORMAL`, `ABUSE_AUTO`, `ABUSE_MANUAL`) |

### Status Code

| 상태 코드 | 설명 |
| --- | --- |
| 200 OK | 상담 종료 성공, 또는 이미 종료된 상담 (현재 종료 정보를 그대로 돌려준다) |
| 400 Bad Request | 상담 번호 형식 오류 (`INVALID_INPUT`) |
| 401 Unauthorized | 인증되지 않은 사용자 (`UNAUTHORIZED`) |
| 403 Forbidden | 수락한 상담원·상담 고객이 아님 (`CHAT_ACCESS_DENIED`) |
| 404 Not Found | 해당 상담이 존재하지 않음 (`CHAT_NOT_FOUND`) |
| 409 Conflict | 아직 상담원과 상담 중이 아님 (`AI_CHAT`·`QUEUED`, `CHAT_NOT_ACTIVE`) |
| 500 Internal Server Error | 서버 내부 오류 |

### 예외 상황

| 상황 | 응답 코드 | errorCode |
| --- | --- | --- |
| 상담 번호 형식이 올바르지 않음 | 400 Bad Request | `INVALID_INPUT` |
| JWT 누락 또는 유효하지 않음 | 401 Unauthorized | `UNAUTHORIZED` |
| 수락한 상담원도, 상담 고객도 아님 (다른 상담원·다른 고객) | 403 Forbidden | `CHAT_ACCESS_DENIED` |
| 해당 상담이 존재하지 않음 | 404 Not Found | `CHAT_NOT_FOUND` |
| 상담이 `AI_CHAT`·`QUEUED` 상태임 (대기 취소는 CUS-04) | 409 Conflict | `CHAT_NOT_ACTIVE` |
| 상담이 이미 `CLOSED`·`TERMINATED` 상태임 | 200 OK | 상태를 바꾸지 않고 현재 종료 정보를 돌려준다 |
| 서버 내부 오류 | 500 Internal Server Error | `INTERNAL_SERVER_ERROR` |

### 처리 규칙

1. 상담 행을 잠그고 조회한다 (MOD-03 폭언 종료·API-005 메시지 저장과 같은 잠금, 체크리스트 E7).
2. 요청자가 수락한 상담원이면 `closedBy = AGENT`, 상담 고객이면 `closedBy = CUSTOMER`. 둘 다 아니면 403.
3. `IN_PROGRESS` 상담을 `CLOSED`로 바꾸고 `closed_at`, `closed_by`, `close_reason = NORMAL`을 기록한다.
4. 이미 `CLOSED`·`TERMINATED`인 상담은 다시 종료하지 않고 200으로 현재 종료 정보를 돌려준다 (두 사람이 동시에 종료해도 안전).
5. 이번 요청으로 실제 종료됐을 때만, 커밋 뒤 `/topic/chats/{chatId}`로 `CONSULTATION_CLOSED`를 발행한다.
6. 종료된 상담에서는 고객과 상담원 간 추가 실시간 메시지 전송을 차단한다 (`CHAT_CLOSED`).
7. 폭언 강제 종료(`TERMINATED`)와 10분 회복 시간은 MOD-03에서 처리한다. 일반 종료에는 회복 시간을 적용하지 않는다.

### 팀 확인 필요 사항

1. **상담원 직접 종료 기능:** 상담원이 종료 버튼을 눌러 직접 종료하는 기능이 최종 정책에 포함되는지 확인.
2. **자동 종료 호출 주체:** 한수민 담당 폭언 처리 기능이 API-006을 호출할지, 내부 Service 메서드를 호출할지 확정.
3. **종료 사유 코드:** `close_reason`에 사용할 실제 값 확정.
4. **종료 주체 코드:** `closed_by`에 사용할 실제 값 확정.
5. **Request Body:** 종료 사유를 클라이언트에서 전달할지 서버에서 결정할지 확정.
6. **고객 종료:** 고객이 직접 상담을 종료하는 경우 동일 API를 사용할지 확인.
7. **상담원 회복시간:** 강제 종료 후 10분 회복시간의 시작 시점과 관리 방식 확인.
8. **종료 상태 알림:** WebSocket을 통해 고객 및 상담원에게 종료 이벤트를 전달하는 방식 확정.
9. **동시 종료 방지:** 상담원 종료와 폭언 자동 종료가 동시에 발생할 경우 상태 변경 처리 방식 확인.
10. **공통 Response Body 형식:** 팀에서 사용하는 응답 구조와 일치 여부 확인.
11. **관련 요구사항 번호:** 최종 요구사항 정의서와 매핑 필요.

---

## API-007 AI 응대 내역 조회

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API ID | API-007 |
| 기능 | AI 메시지 조회 |
| Method | GET |
| URL | /api/chats/{chatId}/ai-messages |
| 사용 화면 | 상담원 콘솔 - 상담 채팅 화면 |
| 호출 시점 | 상담원이 고객 상담을 수락하고 AI 1차 응대 내역을 확인할 때 |
| 인증 필요 | 예 (AGENT) |
| 관련 요구사항 | AI 1차 응대 및 상담원 이관 관련 REQ 번호 확인 필요 |
| 설명 | 상담원에게 이관된 상담의 AI 1차 응대 메시지 내역을 조회한다. 상담원은 고객이 AI와 나눈 대화를 확인하여 이전 문의 내용을 파악하고 상담을 이어갈 수 있다. |

### Request Header

| 이름 | 값 | 필수 | 설명 |
| --- | --- | --- | --- |
| Authorization | Bearer {accessToken} | Y | 상담원 계정 JWT |
| Content-Type | application/json | N | GET 요청으로 별도 Body 없음 |

### Path Variable

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| chatId | Long | Y | 조회할 상담 고유 번호 (`consultation_id`) |

### Request Body

없음 (GET 요청)

### Response Body

아래는 AI 응대 내역을 별도로 반환하는 방식의 예시다. 실제 저장 구조와 응답 DTO는 AI 1차 응대 담당자와 확인이 필요하다.

```json
{
  "success": true,
  "message": "AI 메시지 조회에 성공했습니다.",
  "data": [
    {
      "messageId": 901,
      "consultationId": 101,
      "senderType": "CUSTOMER",
      "content": "배송이 늦어지고 있어요.",
      "createdAt": "2026-11-11T12:55:00"
    },
    {
      "messageId": 902,
      "consultationId": 101,
      "senderType": "AI",
      "content": "배송 지연으로 불편을 드려 죄송합니다. 배송 상태를 확인해 드리겠습니다.",
      "createdAt": "2026-11-11T12:55:10"
    },
    {
      "messageId": 903,
      "consultationId": 101,
      "senderType": "CUSTOMER",
      "content": "상담원과 연결해 주세요.",
      "createdAt": "2026-11-11T12:56:00"
    }
  ]
}
```

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| success | Boolean | Y | 요청 처리 성공 여부 |
| message | String | Y | 처리 결과 메시지 |
| data | Array | Y | AI 1차 응대 메시지 목록 |
| messageId | Long | Y | 메시지 고유 번호 |
| consultationId | Long | Y | 상담 고유 번호 |
| senderType | String | Y | 메시지 발신자 유형 (`CUSTOMER`, `AI` 예시) |
| content | String | Y | 메시지 내용. 폭언이면 마스킹된 내용 |
| createdAt | LocalDateTime | Y | 메시지 생성 시각 |

### Status Code

| 상태 코드 | 설명 |
| --- | --- |
| 200 OK | AI 메시지 조회 성공 |
| 400 Bad Request | 상담 번호 형식 오류 |
| 401 Unauthorized | 인증되지 않은 사용자 |
| 403 Forbidden | 상담원 권한이 없거나 해당 상담에 접근할 권한이 없음 |
| 404 Not Found | 해당 상담이 존재하지 않음 |
| 500 Internal Server Error | 서버 내부 오류 |

### 예외 상황

| 상황 | 응답 코드 | 메시지 |
| --- | --- | --- |
| 상담 번호 형식이 올바르지 않음 | 400 Bad Request | 상담 번호를 확인해주세요. |
| JWT 누락 또는 유효하지 않음 | 401 Unauthorized | 로그인이 필요합니다. |
| 상담원 권한이 없는 사용자 | 403 Forbidden | 접근 권한이 없습니다. |
| 다른 상담원에게 배정된 상담에 접근함 | 403 Forbidden | 해당 상담에 접근할 권한이 없습니다. |
| 상담 번호에 해당하는 상담이 없음 | 404 Not Found | 상담을 찾을 수 없습니다. |
| AI 응대 내역이 없음 | 200 OK | 빈 배열 반환 |
| 서버 내부 오류 | 500 Internal Server Error | 서버 오류가 발생했습니다. |

### 처리 규칙

1. JWT를 통해 상담원 인증 및 권한을 확인한다.
2. `chatId`에 해당하는 상담이 존재하는지 확인한다.
3. 요청한 상담에 접근할 권한이 있는지 확인한다.
4. 해당 상담에서 상담원 이관 이전에 이루어진 AI 1차 응대 메시지를 조회한다.
5. 고객과 AI의 메시지를 시간순으로 반환한다.
6. 폭언으로 판정된 고객 메시지는 마스킹된 내용을 반환한다.
7. `abuse_log.original_content`에 저장된 폭언 원문은 응답에 포함하지 않는다.
8. AI 응대 메시지가 없는 경우 빈 목록을 반환한다.
9. 조회 요청은 상담 상태나 메시지 데이터를 변경하지 않는다.

### 팀 확인 필요 사항

1. **AI 메시지 저장 구조:** 심소민 담당 AI 응대 메시지가 `message` 테이블에 저장되는지 확인.
2. **발신자 유형:** AI 메시지의 `sender_type`에 `AI` 값을 사용하는지 확인.
3. **AI 발신자 식별:** ERD의 `message.sender_id`는 필수 외래키이므로 AI 메시지의 `sender_id`를 어떻게 처리할지 확정.
4. **조회 범위:** `AI_CHAT` 상태에서 생성된 메시지를 구분하는 기준 확인. 현재 ERD에는 메시지별 상담 상태를 기록하는 컬럼이 없음.
5. **API-004와의 중복:** 일반 메시지 내역 조회에 AI 메시지를 포함할지, API-007에서 별도로 조회할지 확인.
6. **조회 정렬:** `created_at` 오름차순 및 동일 시각 메시지 정렬 기준 확인.
7. **폭언 처리:** AI 응대 단계에서도 고객 메시지 마스킹 정책을 동일하게 적용하는지 확인.
8. **공통 Response Body 형식:** 팀의 표준 응답 구조 확인.
9. **관련 요구사항 번호:** 최종 요구사항 정의서와 매핑 필요.
10. **담당자 연동:** 심소민 담당 AI 응대 및 메시지 저장 기능과 조회 방식 협의 필요.

---

## API-008 상담원 회복 시간 조회

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API ID | API-008 |
| 기능 | 상담원 회복시간 조회 |
| Method | GET |
| URL | /api/agents/me/recovery |
| 사용 화면 | 상담원 콘솔 - 상담 대기열 및 상담원 상태 화면 |
| 호출 시점 | 상담원이 콘솔에 진입하거나, 상담 종료 후 회복시간 상태를 확인할 때 |
| 인증 필요 | 예 (AGENT) |
| 관련 요구사항 | 상담원 보호 및 회복시간 관련 REQ 번호 확인 필요 |
| 설명 | 로그인한 상담원의 회복시간 적용 여부와 남은 시간을 조회한다. 폭언에 따른 강제 종료가 발생하면 상담원에게 10분의 회복시간을 적용하고, 회복시간 중에는 새로운 상담을 수락할 수 없도록 한다. |

### Request Header

| 이름 | 값 | 필수 | 설명 |
| --- | --- | --- | --- |
| Authorization | Bearer {accessToken} | Y | 상담원 계정 JWT |
| Content-Type | application/json | N | GET 요청으로 별도 Body 없음 |

### Request Body

없음 (GET 요청)

상담원 식별 정보는 JWT 인증 정보를 통해 확인한다. 별도의 상담원 ID를 요청 본문이나 URL에 전달하지 않는다.

### Response Body

아래는 회복시간 상태를 반환하는 응답 구조 예시다. 실제 응답 DTO와 필드명은 팀에서 확정해야 한다.

**회복시간 적용 중**

```json
{
  "success": true,
  "message": "상담원 회복시간 조회에 성공했습니다.",
  "data": {
    "agentId": 5,
    "isRecovering": true,
    "recoveryStartedAt": "2026-11-11T14:00:00",
    "recoveryEndsAt": "2026-11-11T14:10:00",
    "remainingSeconds": 420,
    "canAcceptChat": false
  }
}
```

**회복시간 미적용 상태**

```json
{
  "success": true,
  "message": "상담원 회복시간 조회에 성공했습니다.",
  "data": {
    "agentId": 5,
    "isRecovering": false,
    "recoveryStartedAt": null,
    "recoveryEndsAt": null,
    "remainingSeconds": 0,
    "canAcceptChat": true
  }
}
```

※ `canAcceptChat`은 회복시간 조건만을 기준으로 한 예시다. 실제 상담 수락 가능 여부는 진행 중인 상담 존재 여부도 함께 확인해야 한다.

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| success | Boolean | Y | 요청 처리 성공 여부 |
| message | String | Y | 처리 결과 메시지 |
| data | Object | Y | 상담원 회복시간 정보 |
| agentId | Long | Y | 로그인한 상담원의 회원 번호 |
| isRecovering | Boolean | Y | 회복시간 적용 여부 |
| recoveryStartedAt | LocalDateTime | N | 회복시간 시작 시각 |
| recoveryEndsAt | LocalDateTime | N | 회복시간 종료 예정 시각 |
| remainingSeconds | Long | Y | 남은 회복시간(초) |
| canAcceptChat | Boolean | Y | 새로운 상담 수락 가능 여부 |

### Status Code

| 상태 코드 | 설명 |
| --- | --- |
| 200 OK | 회복시간 조회 성공 |
| 401 Unauthorized | 인증되지 않은 사용자 |
| 403 Forbidden | 상담원 권한 없음 |
| 500 Internal Server Error | 서버 내부 오류 |

### 예외 상황

| 상황 | 응답 코드 | 메시지 |
| --- | --- | --- |
| JWT 누락 또는 유효하지 않음 | 401 Unauthorized | 로그인이 필요합니다. |
| 상담원 권한이 없는 사용자 | 403 Forbidden | 접근 권한이 없습니다. |
| 회복시간이 적용되지 않은 상담원 | 200 OK | 회복시간 미적용 상태 반환 |
| 회복시간이 이미 만료됨 | 200 OK | 남은 시간 0초 반환 |
| 서버 내부 오류 | 500 Internal Server Error | 서버 오류가 발생했습니다. |

### 처리 규칙

1. JWT를 통해 로그인한 상담원의 회원 번호와 권한을 확인한다.
2. 상담원에게 회복시간이 적용되어 있는지 조회한다.
3. 폭언에 따른 강제 종료(`TERMINATED`) 발생 시 해당 상담원에게 10분의 회복시간을 적용한다.
4. 일반 상담 종료(`CLOSED`)에는 폭언 강제 종료에 따른 회복시간을 자동 적용하지 않는다.
5. 회복시간 종료 예정 시각은 시작 시각으로부터 10분 후로 계산한다.
6. 현재 시각이 회복시간 종료 예정 시각보다 이전이면 회복시간 적용 중으로 판단한다.
7. 회복시간이 적용 중이면 `isRecovering`을 `true`로 반환한다.
8. 남은 시간은 회복시간 종료 예정 시각과 현재 시각의 차이를 초 단위로 계산한다.
9. 남은 시간이 0 이하이면 0으로 반환한다.
10. 회복시간이 적용 중인 상담원은 새로운 상담을 수락할 수 없다.
11. 회복시간이 종료되어도 이미 다른 상담을 진행 중이라면 추가 상담을 수락할 수 없다.
12. 조회 요청 자체는 상담원 상태나 상담 데이터를 변경하지 않는다.

### 팀 확인 필요 사항

1. **회복시간 데이터 저장 방식:** 현재 ERD에 별도의 회복시간 컬럼이 없으므로 `consultation.closed_at`, `consultation.status`, `consultation.agent_id` 등을 이용해 계산할지 확인.
2. **회복시간 시작 기준:** 폭언 강제 종료 시각을 회복시간 시작 시각으로 사용할지 확정.
3. **회복시간 중복 발생:** 이미 회복시간 중인 상담원에게 추가 종료 이벤트가 발생하는 경우 처리 방식 확인.
4. **JWT 인증 정보:** 로그인한 상담원의 `member_id`를 추출하는 방식 확인.
5. **상담 수락 가능 여부:** `canAcceptChat`에 진행 중인 상담 여부까지 반영할지 확정.
6. **회복시간 표시 방식:** 프론트엔드에서 남은 시간을 자체적으로 계산할지, 주기적으로 API를 호출할지 결정.
7. **응답 DTO:** 실제 필드명과 자료형 확정.
8. **공통 Response Body 형식:** 팀의 표준 응답 구조 확인.
9. **관련 요구사항 번호:** 최종 요구사항 정의서와 매핑 필요.
10. **폭언 종료 기능 연동:** 한수민 담당 기능이 `TERMINATED` 상태를 반영한 후 회복시간 조회에 필요한 정보가 정상적으로 기록되는지 확인.
