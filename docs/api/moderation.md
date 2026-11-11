# API 명세서 — 폭언 감지·경고

[← API 명세서 목차](README.md)

| API ID | Method | URL | 기능명 | 분류 | 인증 | 상태 | 담당자 | 비고 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| MOD-01 | INTERNAL | `ModerationService.processMessage()` | 폭언 감지 및 메시지 마스킹 | 내부 | 불필요 (서버 내부 호출, 고객 메시지 송신 시 JWT 인증은 선행) | 작성완료 | 한수민 |  |
| MOD-02 | INTERNAL | `ModerationService.applyAutoWarning()` | 자동 경고 처리 | 내부 | 불필요 (서버 내부 호출) | 작성완료 | 한수민 |  |
| MOD-03 | INTERNAL | `ModerationService.terminateForAbuse()` | 폭언 누적 상담 자동 종료 | 내부 | 불필요 (서버 내부 이벤트) | 작성완료 | 한수민 |  |
| MOD-04 | EVENT | `AbuseConsultationTerminatedEvent` | 위반 기록 및 자동 제재 | 내부 | 불필요(서버 내부) | 작성완료 | 한수민 |  |
| MOD-05 | GET | `/api/consultations/{consultationId}/warnings` | 상담별 경고 내역 조회 | 상담 | 필요 (AGENT, ADMIN) | 작성완료 | 한수민 |  |
| MOD-06 | POST | `/api/consultations/{consultationId}/warnings` | 상담원 수동 경고 발송 | 상담 | 필요 (AGENT) | 작성완료 | 한수민 |  |
| MOD-07 | PATCH | `/api/consultations/{consultationId}/terminate` | 상담원 수동 강제 종료 | 상담 | 필요 (AGENT, ADMIN) | 작성완료 | 한수민 |  |

---

## MOD-01 폭언 감지 및 메시지 마스킹

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API ID | MOD-01 |
| 분류 | 내부 |
| 기능 | 폭언 감지 및 메시지 마스킹 |
| Method | INTERNAL |
| URL | `ModerationService.processMessage()` |
| 사용 화면 | 고객 실시간 상담 / 상담원 실시간 상담 |
| 인증 필요 | 불필요 (서버 내부, WebSocket 인증 선행) |
| 담당자 | 한수민 |
| 상태 | 작성완료 |
| 우선순위 | Must |
| 설명 | 고객 메시지를 금칙어 및 정규식으로 검사하여 NORMAL 또는 ABUSIVE로 분류한다. 폭언으로 판정된 표현은 마스킹하고 원문은 별도로 보관한다. |

### 요청

**내부 메서드 입력 예시**

```java
ModerationResult processMessage(
    Long consultationId,
    Long senderId,
    String content
);
```

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| consultationId | Long | Y | 상담 ID |
| senderId | Long | Y | 고객 회원 ID |
| content | String | Y | 고객 메시지 원문 |

### 응답

**NORMAL**

```json
{
  "success": true,
  "data": {
    "consultationId": 101,
    "abuseStatus": "NORMAL",
    "maskedContent": "환불 처리가 언제 되나요?",
    "detected": false
  },
  "message": "메시지 검사가 완료되었습니다."
}
```

**ABUSIVE**

```json
{
  "success": true,
  "data": {
    "consultationId": 101,
    "abuseStatus": "ABUSIVE",
    "maskedContent": "상담원 진짜 ***하네",
    "detected": true
  },
  "message": "폭언이 감지되었습니다."
}
```

위 응답은 내부 결과 DTO를 설명하기 위한 JSON 표현이다. 외부 REST 응답은 아니다.

### 처리 순서

1. WebSocket에서 고객 메시지를 수신한다.
2. 상담 상태가 ACTIVE인지 확인한다.
3. 발신자가 해당 상담의 고객인지 확인한다.
4. 메시지를 정규화한다.
5. `banned_word` 테이블의 금칙어를 검사한다.
6. 정규식으로 띄어쓰기·특수문자·초성 변형을 검사한다.
7. 인용·신고 등 오탐 가능 문맥을 확인한다.
8. NORMAL 또는 ABUSIVE로 판정한다.
9. NORMAL은 원문을 `message.content`에 저장한다.
10. ABUSIVE는 마스킹된 문장을 `message.content`에 저장하고 원문을 `abuse_log.original_content`에 저장한다.
11. 상담원에게 마스킹된 메시지를 전달한다.
12. ABUSIVE이면 MOD-02를 실행한다.

### 예외 상황

| 상황 | errorCode | 처리 |
| --- | --- | --- |
| 빈 메시지 | INVALID_MESSAGE | 전송 거부 |
| 상담 없음 | CONSULTATION_NOT_FOUND | 처리 중단 |
| 상담 진행 중 아님 | CHAT_NOT_ACTIVE | 전송 거부 |
| 고객 불일치 | FORBIDDEN_CUSTOMER | 접근 거부 |
| 금칙어 조회 실패 | MODERATION_UNAVAILABLE | 원문 노출 없이 전송 보류 |
| 메시지 중복 수신 | DUPLICATE_MESSAGE | 중복 처리 방지 |
| 저장 실패 | MESSAGE_SAVE_FAILED | 트랜잭션 롤백 |

### DB 연동

| 테이블 | 컬럼 | 작업 |
| --- | --- | --- |
| consultation | consultation_id, status, customer_id | SELECT |
| banned_word | word | SELECT |
| message | content, abuse_status, consultation_id, sender_id | INSERT |
| abuse_log | original_content, message_id, consultation_id | INSERT |

### 설계 이유

- 서버에서 검사하여 클라이언트 우회를 방지한다.
- 상담원에게 폭언 원문을 노출하지 않는다.
- 원문과 마스킹 메시지를 분리해 관리자 확인과 상담원 보호를 동시에 지원한다.
- DB 금칙어와 정규식을 함께 사용하여 변형 표현을 감지한다.
- 단순 항의나 인용 표현의 오탐을 줄이기 위한 예외 규칙을 둔다.
- 폭언 감지 실패 시 원문이 상담원에게 그대로 전달되지 않도록 한다.

---

## MOD-02 자동 경고 처리

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API ID | MOD-02 |
| 분류 | 내부 |
| 기능 | 자동 경고 처리 |
| Method | INTERNAL |
| URL | `ModerationService.applyAutoWarning()` |
| 사용 화면 | 고객 실시간 상담 / 상담원 실시간 상담 |
| 인증 필요 | 불필요 (서버 내부) |
| 담당자 | 한수민 |
| 상태 | 작성완료 |
| 우선순위 | Must |
| 설명 | ABUSIVE 메시지가 감지되면 자동 경고를 기록하고 고객에게 경고 알림을 전송한다. |

### 요청

```java
WarningResult applyAutoWarning(
    Long consultationId,
    Long abuseLogId
);
```

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| consultationId | Long | Y | 상담 ID |
| abuseLogId | Long | Y | 폭언 기록 ID |

### 응답

```json
{
  "success": true,
  "data": {
    "consultationId": 101,
    "warningId": 501,
    "warningType": "AUTO",
    "warningCount": 1,
    "closeThreshold": 2,
    "shouldTerminate": false
  },
  "message": "자동 경고가 기록되었습니다."
}
```

### 처리 순서

1. MOD-01에서 ABUSIVE 결과를 전달받는다.
2. 상담 상태가 ACTIVE인지 확인한다.
3. 해당 폭언 기록으로 경고가 이미 생성되었는지 확인한다.
4. 활성 `sanction_policy`의 종료 기준을 조회한다.
5. `warning` 테이블에 AUTO 경고를 저장한다.
6. 해당 상담의 자동 경고 횟수를 계산한다.
7. 고객에게 경고 이벤트를 발송한다.
8. 종료 기준에 도달하면 MOD-03을 실행한다.

### 예외 상황

| 상황 | errorCode | 처리 |
| --- | --- | --- |
| 상담 없음 | CONSULTATION_NOT_FOUND | 처리 중단 |
| 폭언 기록 없음 | ABUSE_LOG_NOT_FOUND | 처리 중단 |
| 중복 경고 | DUPLICATE_WARNING | 추가 저장하지 않음 |
| 상담 종료 상태 | CHAT_ALREADY_CLOSED | 경고 생성 중단 |
| 정책 조회 실패 | SANCTION_POLICY_NOT_FOUND | 종료 판단 보류 |
| 경고 저장 실패 | WARNING_SAVE_FAILED | 트랜잭션 롤백 |

### DB 연동

| 테이블 | 컬럼 | 작업 |
| --- | --- | --- |
| consultation | consultation_id, status | SELECT |
| warning | consultation_id, abuse_log_id, warning_type, created_at | SELECT, INSERT |
| sanction_policy | warn_threshold, close_threshold | SELECT |

### WebSocket 알림 예시

```json
{
  "type": "WARNING_ISSUED",
  "consultationId": 101,
  "warningId": 501,
  "warningCount": 1,
  "message": "부적절한 표현이 감지되었습니다. 반복 시 상담이 종료될 수 있습니다."
}
```

### 설계 이유

- 자동 경고와 수동 경고를 구분한다.
- 상담별 폭언 횟수를 계산해 이전 상담의 경고가 새 상담에 영향을 주지 않도록 한다.
- 경고 내역을 DB에 저장해 재접속 시에도 확인할 수 있도록 한다.
- 경고 기준을 정책 테이블에서 조회해 정책 변경을 반영한다.
- 중복 메시지로 경고가 여러 번 발생하지 않도록 한다.

---

## MOD-03 폭언 누적 상담 자동 종료

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API ID | MOD-03 |
| 분류 | 내부 |
| 기능 | 폭언 누적 상담 자동 종료 |
| Method | INTERNAL |
| URL | `ModerationService.terminateForAbuse()` |
| 사용 화면 | 고객 실시간 상담 / 상담원 실시간 상담 |
| 인증 필요 | 불필요 (서버 내부) |
| 담당자 | 한수민 |
| 상태 | 작성완료 |
| 우선순위 | Must |
| 설명 | 동일 상담에서 폭언이 종료 기준에 도달하면 상담을 강제 종료하고 위반·제재 처리 이벤트를 발생시킨다. |

### 요청

```java
TerminationResult terminateForAbuse(
    Long consultationId
);
```

### 응답

```json
{
  "success": true,
  "data": {
    "consultationId": 101,
    "status": "TERMINATED",
    "closeReason": "ABUSE_THRESHOLD_EXCEEDED",
    "closedAt": "2026-11-11T13:20:00"
  },
  "message": "폭언 누적으로 상담이 종료되었습니다."
}
```

### 처리 순서

1. 해당 상담의 자동 경고 횟수를 조회한다.
2. 활성 정책의 `close_threshold`를 조회한다.
3. 자동 경고 횟수가 종료 기준 이상인지 확인한다.
4. 상담 상태가 ACTIVE인지 확인한다.
5. 상담 상태를 TERMINATED로 변경한다.
6. 종료 시각과 종료 사유를 저장한다.
7. 고객과 상담원에게 종료 이벤트를 발송한다.
8. MOD-04 위반 기록 및 자동 제재를 실행한다.
9. 기존 INT-01 상담 요약 생성 기능에도 상담 종료 사실을 전달한다.

### 예외 상황

| 상황 | errorCode | 처리 |
| --- | --- | --- |
| 상담 없음 | CONSULTATION_NOT_FOUND | 처리 중단 |
| 경고 기준 미달 | WARNING_THRESHOLD_NOT_MET | 자동 종료하지 않음 |
| 이미 종료된 상담 | CHAT_ALREADY_CLOSED | 중복 종료 방지 |
| 정책 없음 | SANCTION_POLICY_NOT_FOUND | 종료 판단 보류 |
| 상태 변경 실패 | CHAT_TERMINATION_FAILED | 트랜잭션 롤백 |

### DB 연동

| 테이블 | 컬럼 | 작업 |
| --- | --- | --- |
| consultation | status, closed_at, close_reason, closed_by | UPDATE |
| warning | consultation_id, warning_type | SELECT |
| sanction_policy | close_threshold | SELECT |

### WebSocket 알림 예시

```json
{
  "type": "CONSULTATION_TERMINATED",
  "consultationId": 101,
  "status": "TERMINATED",
  "reason": "반복적인 폭언으로 상담이 종료되었습니다.",
  "closedAt": "2026-11-11T13:20:00"
}
```

### 설계 이유

- 종료 기준을 서버에서 판단해 프론트엔드 조작을 방지한다.
- 종료 상태 변경과 후속 이벤트 처리를 일관되게 관리한다.
- 동일 상담의 중복 종료를 방지한다.
- 기존 상담 요약 생성 기능과 연동한다.
- 자동 종료와 상담원의 수동 종료를 구분해 제재 정책을 명확히 한다.

---

## MOD-04 위반 기록 및 자동 제재

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API ID | MOD-04 |
| 분류 | 내부 |
| 기능 | 위반 기록 및 자동 제재 |
| Method | EVENT |
| URL | `AbuseConsultationTerminatedEvent` |
| 사용 화면 | 고객 이용정지 안내 / 관리자 제재 관리 |
| 인증 필요 | 불필요 (서버 내부) |
| 담당자 | 한수민 |
| 상태 | 작성완료 |
| 우선순위 | Must |
| 설명 | 폭언으로 상담이 자동 종료되면 위반 이력을 생성하고 활성 제재 정책에 따라 고객에게 이용정지를 적용한다. |

### 요청

**내부 이벤트**

```json
{
  "consultationId": 101,
  "customerId": 10,
  "closeReason": "ABUSE_THRESHOLD_EXCEEDED",
  "closedAt": "2026-11-11T13:20:00"
}
```

### 응답

```json
{
  "success": true,
  "data": {
    "violationId": 201,
    "customerId": 10,
    "totalViolations": 1,
    "sanctionId": 301,
    "suspendDays": 7,
    "startAt": "2026-11-11T13:20:00",
    "endAt": "2026-11-18T13:20:00"
  },
  "message": "위반 기록 및 자동 제재가 적용되었습니다."
}
```

### 처리 순서

1. 폭언 자동 종료 이벤트를 수신한다.
2. 종료 사유가 ABUSE_THRESHOLD_EXCEEDED인지 확인한다.
3. 동일 상담에 위반 이력이 존재하는지 확인한다.
4. `violation` 테이블에 위반 기록을 생성한다.
5. 고객의 누적 위반 횟수를 계산한다.
6. 활성 `sanction_policy`를 조회한다.
7. 누적 위반 횟수에 따라 정지 일수를 결정한다.
8. `sanction` 테이블에 제재를 등록한다.
9. 고객에게 제재 결과를 알린다.
10. CUS-01과 HIS-02에서 제재 상태를 확인할 수 있도록 연동한다.

### 기본 정책

| 누적 위반 | 기본 정지 기간 |
| --- | --- |
| 1회 | 7일 |
| 2회 | 30일 |
| 3회 이상 | 30일 및 관리자 검토 |

기간은 고정하지 않고 활성 정책의 `first_days`, `second_days`, `third_days`를 적용한다.

### 예외 상황

| 상황 | errorCode | 처리 |
| --- | --- | --- |
| 상담 없음 | CONSULTATION_NOT_FOUND | 처리 중단 |
| 고객 없음 | CUSTOMER_NOT_FOUND | 처리 중단 |
| 폭언 종료 아님 | INVALID_TERMINATION_REASON | 위반 생성하지 않음 |
| 위반 중복 | DUPLICATE_VIOLATION | 중복 생성 방지 |
| 정책 없음 | SANCTION_POLICY_NOT_FOUND | 제재 적용 보류 |
| DB 저장 실패 | SANCTION_SAVE_FAILED | 재처리 가능한 오류로 기록 |

### DB 연동

| 테이블 | 컬럼 | 작업 |
| --- | --- | --- |
| consultation | consultation_id, customer_id, close_reason | SELECT |
| violation | consultation_id, customer_id, created_at | INSERT |
| sanction_policy | first_days, second_days, third_days | SELECT |
| sanction | violation_id, customer_id, suspend_days, start_at, end_at | INSERT |

### 설계 이유

- 위반은 개별 폭언마다 기록하지 않고 폭언에 따른 상담 강제 종료를 기준으로 기록한다.
- 고객 단위의 누적 위반 횟수로 제재 기간을 계산한다.
- 제재 기간을 DB 정책에서 조회해 관리자 정책 변경을 반영한다.
- 동일 상담에 대해 위반·제재가 중복 생성되지 않도록 한다.
- 제재 상태 판정 로직을 CUS-01과 HIS-02에서 재사용한다.
- 상담 종료, 위반 생성, 제재 저장이 중간 실패로 불일치하지 않도록 트랜잭션과 이벤트 처리 방식을 협의한다.

---

## MOD-05 상담별 경고 내역 조회

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API ID | MOD-05 |
| 분류 | 상담 |
| 기능 | 상담별 경고 내역 조회 |
| Method | GET |
| URL | `/api/consultations/{consultationId}/warnings` |
| 사용 화면 | 상담원 실시간 상담 / 관리자 상담 상세 |
| 인증 필요 | 예 (AGENT, ADMIN) |
| 담당자 | 한수민 |
| 상태 | 작성완료 |
| 우선순위 | Must |
| 설명 | 특정 상담에서 발생한 자동·수동 경고 이력과 누적 경고 횟수를 조회한다. |

### 요청

**Path Parameter**

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| consultationId | Long | Y | 상담 ID |

Request Body 없음.

### 응답

**200 OK**

```json
{
  "success": true,
  "data": {
    "consultationId": 101,
    "warningCount": 1,
    "autoWarningCount": 1,
    "closeThreshold": 2,
    "warnings": [
      {
        "warningId": 501,
        "warningType": "AUTO",
        "createdAt": "2026-11-11T13:10:00"
      }
    ]
  },
  "message": "경고 내역 조회에 성공했습니다."
}
```

### 처리 순서

1. JWT에서 사용자 ID와 역할을 확인한다.
2. 상담 존재 여부를 확인한다.
3. AGENT인 경우 해당 상담 담당자인지 검증한다.
4. 해당 상담의 경고 이력을 조회한다.
5. 자동 경고와 수동 경고를 구분해 횟수를 계산한다.
6. 활성 정책의 종료 기준을 조회한다.
7. 경고 목록과 누적 횟수를 반환한다.

### 예외 상황

| 상황 | HTTP | errorCode | 처리 |
| --- | --- | --- | --- |
| 비로그인 | 401 | UNAUTHORIZED | 로그인 화면으로 이동 |
| 고객 계정 | 403 | FORBIDDEN_ROLE | 접근 거부 |
| 담당 상담원 아님 | 403 | NOT_ASSIGNED_AGENT | 조회 차단 |
| 상담 없음 | 404 | CONSULTATION_NOT_FOUND | 상담 없음 안내 |

### DB 연동

| 테이블 | 컬럼 | 작업 |
| --- | --- | --- |
| consultation | consultation_id, agent_id | SELECT |
| warning | warning_id, consultation_id, warning_type, created_at | SELECT |
| sanction_policy | close_threshold | SELECT |

### 설계 이유

- 서버에서 경고 횟수를 계산해 화면 간 불일치를 방지한다.
- 자동·수동 경고를 구분해 정책 적용 기준을 명확히 한다.
- 상담원은 본인이 담당하는 상담의 경고만 조회하도록 한다.
- 폭언 원문은 반환하지 않는다.
- 실시간 화면 재접속 시에도 경고 상태를 복원할 수 있도록 한다.

---

## MOD-06 상담원 수동 경고 발송

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API ID | MOD-06 |
| 분류 | 상담 |
| 기능 | 상담원 수동 경고 발송 |
| Method | POST |
| URL | `/api/consultations/{consultationId}/warnings` |
| 사용 화면 | 상담원 실시간 상담 |
| 인증 필요 | 예 (AGENT) |
| 담당자 | 한수민 |
| 상태 | 작성완료 |
| 우선순위 | Should |
| 설명 | 상담원이 고객에게 직접 경고를 발송하고 수동 경고 이력을 저장한다. |

### 요청

**Path Parameter**

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| consultationId | Long | Y | 상담 ID |

**Request Body**

```json
{
  "reason": "상담 진행을 방해하는 반복적인 부적절한 표현"
}
```

### 응답

**201 Created**

```json
{
  "success": true,
  "data": {
    "warningId": 502,
    "consultationId": 101,
    "warningType": "MANUAL",
    "createdAt": "2026-11-11T13:15:00"
  },
  "message": "수동 경고가 발송되었습니다."
}
```

### 처리 순서

1. JWT에서 상담원 ID를 조회한다.
2. 상담 존재 여부를 확인한다.
3. 해당 상담 담당자인지 확인한다.
4. 상담 상태 ACTIVE를 확인한다.
5. 경고 사유를 검증한다.
6. `warning` 테이블에 MANUAL 경고를 저장한다.
7. 고객에게 경고 WebSocket 이벤트를 발송한다.
8. 처리 결과를 반환한다.

### 예외 상황

| 상황 | HTTP | errorCode | 처리 |
| --- | --- | --- | --- |
| 비로그인 | 401 | UNAUTHORIZED | 로그인 요청 |
| 상담원 아님 | 403 | FORBIDDEN_ROLE | 접근 거부 |
| 담당 상담원 아님 | 403 | NOT_ASSIGNED_AGENT | 경고 거부 |
| 상담 없음 | 404 | CONSULTATION_NOT_FOUND | 상담 없음 안내 |
| 상담 진행 중 아님 | 409 | CHAT_NOT_ACTIVE | 경고 불가 |
| 경고 사유 누락 | 400 | INVALID_REASON | 입력 요청 |

### DB 연동

| 테이블 | 컬럼 | 작업 |
| --- | --- | --- |
| consultation | consultation_id, agent_id, status | SELECT |
| warning | consultation_id, warning_type, issued_by, created_at | INSERT |

### 설계 이유

- 자동 감지와 별도로 상담원이 경고할 수 있도록 한다.
- 수동 경고를 AUTO와 구분해 기록한다.
- 담당 상담원만 경고를 발송할 수 있도록 한다.
- 경고 기록을 저장해 추후 관리자 확인이 가능하도록 한다.
- 수동 경고를 자동 종료 횟수에 포함할지는 정책 확정 후 적용한다.

---

## MOD-07 상담원 수동 강제 종료

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API ID | MOD-07 |
| 분류 | 상담 |
| 기능 | 상담원 수동 강제 종료 |
| Method | PATCH |
| URL | `/api/consultations/{consultationId}/terminate` |
| 사용 화면 | 상담원 실시간 상담 / 관리자 상담 관리 |
| 인증 필요 | 예 (AGENT, ADMIN) |
| 담당자 | 한수민 |
| 상태 | 작성완료 |
| 우선순위 | Should |
| 설명 | 상담원 또는 관리자가 진행 중인 상담을 수동으로 강제 종료한다. 자동 폭언 종료와 구분해 처리한다. |

### 요청

**Path Parameter**

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| consultationId | Long | Y | 상담 ID |

**Request Body**

```json
{
  "reason": "반복적인 부적절한 행동으로 상담 진행 불가"
}
```

### 응답

**200 OK**

```json
{
  "success": true,
  "data": {
    "consultationId": 101,
    "status": "TERMINATED",
    "closeReason": "반복적인 부적절한 행동으로 상담 진행 불가",
    "closedAt": "2026-11-11T13:20:00"
  },
  "message": "상담이 강제 종료되었습니다."
}
```

### 처리 순서

1. JWT 인증과 역할을 확인한다.
2. 상담 존재 여부를 확인한다.
3. AGENT인 경우 담당 상담원인지 확인한다.
4. 상담 상태 ACTIVE를 확인한다.
5. 종료 사유를 검증한다.
6. 상담 상태를 TERMINATED로 변경한다.
7. 종료 시각·사유·처리자를 저장한다.
8. 고객과 상담원에게 종료 이벤트를 발송한다.
9. 결과를 반환한다.

### 예외 상황

| 상황 | HTTP | errorCode | 처리 |
| --- | --- | --- | --- |
| 비로그인 | 401 | UNAUTHORIZED | 로그인 요청 |
| 권한 없음 | 403 | FORBIDDEN_ROLE | 접근 거부 |
| 담당 상담원 아님 | 403 | NOT_ASSIGNED_AGENT | 종료 거부 |
| 상담 없음 | 404 | CONSULTATION_NOT_FOUND | 상담 없음 안내 |
| 이미 종료된 상담 | 409 | CHAT_ALREADY_CLOSED | 중복 종료 방지 |
| 종료 사유 누락 | 400 | INVALID_REASON | 입력 요청 |

### DB 연동

| 테이블 | 컬럼 | 작업 |
| --- | --- | --- |
| consultation | status, closed_at, closed_by, close_reason | UPDATE |

### 설계 이유

- 상담원이 필요할 때 직접 상담을 종료할 수 있도록 한다.
- 자동 종료와 수동 종료를 구분해 위반·제재 정책을 명확히 한다.
- 종료 사유와 처리자를 저장해 추후 관리자 확인이 가능하도록 한다.
- 이미 종료된 상담의 중복 종료를 방지한다.
- 수동 종료에 따른 위반·제재 적용은 별도 정책이 확정되기 전까지 자동 실행하지 않는다.
