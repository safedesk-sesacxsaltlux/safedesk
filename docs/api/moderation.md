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
| 설명 | 고객 메시지를 금칙어로 검사하여 NORMAL 또는 ABUSIVE로 분류한다. 폭언으로 판정된 표현은 마스킹하고 원문은 별도로 보관한다. |

### 요청

판정과 원문 저장을 두 메서드로 나눈다. `abuse_log`에는 `message_id`가 필요한데, 이 값은 메시지를 저장한 뒤에야 생기기 때문이다.

```java
// ① 판정. 메시지 저장 「전」에 호출
AbuseFilterResult processMessage(String content);

// ② 원문 저장. 메시지 저장 「뒤」, ABUSIVE일 때만 호출
AbuseLog saveOriginal(long messageId, long consultationId, String originalContent);
```

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| content | String | Y | 고객 메시지 원문 |
| messageId | long | Y | 저장된 메시지 ID |
| consultationId | long | Y | 상담 ID |
| originalContent | String | Y | 폭언 원문 |

상담 상태와 발신자 확인은 호출하는 쪽(`ChatService.sendMessage()`)이 한다. 필터는 문장만 본다.

### 응답

`processMessage()`는 `AbuseFilterResult`를 돌려준다.

**NORMAL**

```json
{
  "abuseStatus": "NORMAL",
  "maskedContent": "환불 처리가 언제 되나요?"
}
```

**ABUSIVE**

```json
{
  "abuseStatus": "ABUSIVE",
  "maskedContent": "상담원 진짜 ***하네"
}
```

| 필드 | 설명 |
| --- | --- |
| abuseStatus | `NORMAL` 또는 `ABUSIVE` |
| maskedContent | NORMAL이면 원문 그대로, ABUSIVE이면 금칙어 구간만 `***`로 바꾼 문장 |

`detected()`는 `abuseStatus == ABUSIVE`이면 true다. `saveOriginal()`은 저장된 `AbuseLog`를 돌려주고, MOD-02가 그 ID로 경고를 연결한다.

위 응답은 내부 결과 DTO를 설명하기 위한 JSON 표현이다. 외부 REST 응답은 아니다.

### 처리 순서

`ChatService.sendMessage()` 한 트랜잭션 안에서 실행한다.

1. 상담을 조회한다. 없으면 `CHAT_NOT_FOUND`.
2. 상담이 `IN_PROGRESS`이고 고객이 보낸 메시지일 때만 검사한다. AI 상담 중 메시지와 상담원 메시지는 검사하지 않고 `abuse_status`를 NULL로 둔다 (정책 4장, 체크리스트 E11).
3. 메시지를 정규화한다. 공백·특수문자를 지우고 영문은 소문자로 바꾼다 (「시 발」, 「ㅆ!발」 대응). 원문 위치는 따로 기록해 둔다.
4. 메모리에 캐싱한 금칙어(`banned_word`)가 정규화 문장에 있는지 찾는다.
5. 오탐 예외 표현(「다시 발송」, 「시발점」, 「질병 신고」 등) 안에 완전히 들어간 금칙어는 무시한다.
6. 금칙어가 남아 있으면 ABUSIVE, 없으면 NORMAL로 판정한다. ABUSIVE는 겹치는 구간을 합친 뒤 원문 기준으로 `***` 치환한다.
7. `message.content`에 마스킹본(NORMAL은 원문)을, `message.abuse_status`에 판정 결과를 저장한다.
8. ABUSIVE이면 원문을 `abuse_log.original_content`에 저장한다.
9. ABUSIVE이면 MOD-02를 실행한다.
10. 트랜잭션이 커밋된 뒤 `/topic/chats/{consultationId}`로 저장된 메시지(`MESSAGE`)를 발행한다. 원문 필드는 없다.

### 예외 상황

| 상황 | errorCode | 처리 |
| --- | --- | --- |
| 상담 없음 | CHAT_NOT_FOUND | 처리 중단 |
| 빈 메시지 | - | 필터는 NORMAL로 돌려준다. 빈 메시지 거부는 호출부 책임 |
| 금칙어 적재 실패 | - | 앱 시작 시 적재하며, 실패하면 앱이 뜨지 않는다. 금칙어 없이 상담이 열리면 원문이 그대로 노출되기 때문이다 |
| 필터·저장 중 예외 | - | 트랜잭션 롤백. 커밋 후 발행이므로 메시지가 저장·발행되지 않는다 (원문 노출 없이 전송 보류) |

### DB 연동

| 테이블 | 컬럼 | 작업 |
| --- | --- | --- |
| consultation | consultation_id, status | SELECT |
| banned_word | word | SELECT (앱 시작 시 한 번, 메모리 캐싱) |
| message | content, abuse_status, consultation_id, sender_type, sender_id | INSERT |
| abuse_log | message_id, consultation_id, original_content | INSERT |

### 설계 이유

- 서버에서 검사하여 클라이언트 우회를 방지한다.
- 상담원에게 폭언 원문을 노출하지 않는다. 발행하는 메시지는 클라이언트가 보낸 요청이 아니라 저장된 마스킹본이다.
- 원문과 마스킹 메시지를 분리해 관리자 확인과 상담원 보호를 동시에 지원한다.
- 정규화로 띄어쓰기·특수문자 변형을 잡고, 금칙어는 메모리에 두어 메시지마다 DB를 조회하지 않는다 (NFR-007 전달 지연 1초 이내).
- 정규화로 생기는 오탐(「다시 발송」 → 「다시발송」)은 예외 표현으로 막는다.
- 커밋 후 발행하여, 폭언 감지 실패 시 원문이 상담원에게 그대로 전달되지 않도록 한다.

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
Optional<WarningResult> applyAutoWarning(
    long consultationId,
    long abuseLogId
);
```

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| consultationId | long | Y | 상담 ID |
| abuseLogId | long | Y | 폭언 기록 ID (MOD-01 `saveOriginal()`이 돌려준 `AbuseLog`의 ID) |

`ChatService.sendMessage()`가 MOD-01 원문 저장 뒤에 같은 트랜잭션 안에서 호출한다.

### 응답

```json
{
  "consultationId": 101,
  "warningId": 501,
  "warningCount": 1,
  "closeThreshold": 2,
  "shouldTerminate": false
}
```

| 필드 | 설명 |
| --- | --- |
| warningCount | 이 상담의 경고 횟수. 방금 저장한 경고를 포함하고, AUTO와 MANUAL을 모두 센다 (정책 4장) |
| closeThreshold | 상담에 적용된 정책의 자동 종료 기준 |
| shouldTerminate | `warningCount >= closeThreshold`. true이면 MOD-03 대상 |

경고를 만들지 않은 경우(중복 경고, 진행 중이 아닌 상담)에는 `Optional.empty()`를 돌려준다. 예외를 던지면 같은 트랜잭션의 메시지 저장까지 롤백되기 때문이다.

위 응답은 내부 결과 DTO(`WarningResult`)를 설명하기 위한 JSON 표현이다. 외부 REST 응답은 아니다.

### 처리 순서

1. 상담을 조회한다.
2. 상담 상태가 `IN_PROGRESS`가 아니면 경고하지 않는다 (`Optional.empty()`).
3. 해당 폭언 기록으로 경고가 이미 있으면 경고하지 않는다 (`Optional.empty()`).
4. **상담에 저장된 `policy_id`의 정책**을 조회한다. 정책을 바꿔도 진행 중인 상담에는 시작할 때의 정책이 적용된다 (체크리스트 S3). `policy_id`가 비어 있거나 찾지 못하면 가장 최근 정책을 쓴다.
5. `warning` 테이블에 AUTO 경고를 저장한다.
6. 해당 상담의 경고 횟수(AUTO + MANUAL)를 센다.
7. `WarningResult`를 돌려준다.

`ChatService`는 결과를 받아 다음을 처리한다.

8. `shouldTerminate`가 false이면 `WARNING_ISSUED`를 발행한다. 마스킹된 `MESSAGE`가 먼저, 경고가 다음에 도착한다. 둘 다 트랜잭션 커밋 후에 발행한다.
9. `shouldTerminate`가 true이면 경고 알림을 보내지 않고 MOD-03을 실행한다. 이때 고객에게는 MOD-03의 종료 알림만 간다.

### 예외 상황

| 상황 | errorCode | 처리 |
| --- | --- | --- |
| 상담 없음 | CHAT_NOT_FOUND | 처리 중단, 트랜잭션 롤백 |
| 상담 진행 중 아님 | - | 경고하지 않음 (`Optional.empty()`) |
| 중복 경고 | - | 추가 저장하지 않음 (`Optional.empty()`). DB에도 `uk_warning_abuse_log_id` 유니크 제약이 있다 |
| 정책 없음 | - | `IllegalStateException`. 시드에 기본 정책이 항상 있으므로 DB 설정 오류로 본다 |
| 경고 저장 실패 | - | 트랜잭션 롤백. 메시지·원문도 저장되지 않고 발행하지 않는다 |

### DB 연동

| 테이블 | 컬럼 | 작업 |
| --- | --- | --- |
| consultation | consultation_id, status, policy_id | SELECT |
| warning | consultation_id, abuse_log_id, warning_type | SELECT, INSERT |
| sanction_policy | policy_id, close_threshold | SELECT |

### WebSocket 알림 예시

`/topic/chats/{consultationId}`로 발행한다 (contract.md WebSocket 「WARNING_ISSUED」).

```json
{
  "type": "WARNING_ISSUED",
  "consultationId": 101,
  "warningId": 501,
  "warningCount": 1,
  "message": "부적절한 표현이 감지되었습니다. 한 번 더 폭언 시 상담이 종료되고 이용이 제한될 수 있습니다."
}
```

- 문구의 남은 횟수는 `closeThreshold - warningCount`로 정한다. 기본 정책(2회)의 첫 경고는 "한 번 더"이고, 기준이 3회이면 첫 경고는 "2번 더"가 된다 (REQ-016, 정책 4장).
- 원문은 넣지 않는다. 같은 토픽을 상담원도 구독한다 (NFR-005).

### 설계 이유

- 자동 경고와 수동 경고를 구분하되, 횟수는 함께 센다 (정책 4장 「수동 경고도 1회로 셈」).
- 상담별 폭언 횟수를 계산해 이전 상담의 경고가 새 상담에 영향을 주지 않도록 한다.
- 경고 내역을 DB에 저장해 재접속 시에도 확인할 수 있도록 한다.
- 정책은 상담에 저장된 `policy_id`로 조회한다. 관리자가 기준을 바꿔도 진행 중인 상담의 기준이 중간에 바뀌지 않는다.
- 중복 메시지로 경고가 여러 번 발생하지 않도록 한다.
- 경고 처리는 메시지 저장과 같은 트랜잭션에서 실행하고 커밋 후에 발행한다. 경고 처리가 실패하면 메시지도 저장·발행되지 않아, 경고 없이 폭언 메시지만 전달되는 일이 없다.

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
