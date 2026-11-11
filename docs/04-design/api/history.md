# API 명세서 — 상담 이력·통계

> 출처: Notion 「API 명세서」 하위 행 HIS-01 ~ HIS-07 (마지막 수정: 2026-11-11)

[← API 명세서 목차](../api-spec.md)

| API ID | Method | URL | 기능명 | 분류 | 인증 | 상태 | 담당자 | 비고 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| HIS-01 | GET | `/api/customers/me/consultations` | 내 상담 이력 목록 | 상담 이력 | 필요 | 작성완료 | 유능한 |  |
| HIS-02 | GET | `/api/customers/me/sanction` | 내 이용 상태(제재) 조회 | 상담 이력 | 필요 | 작성완료 | 유능한 |  |
| HIS-03 | GET | `/api/agents/me/customers` | 응대 고객별 이력 | 상담 이력 | 필요 | 작성완료 | 유능한 |  |
| HIS-04 | GET | `/api/agents/me/consultations` | 내가 맡은 상담 목록 | 상담 이력 | 필요 | 작성완료 | 유능한 |  |
| HIS-05 | GET | `/api/admin/consultations` | 전체 상담 이력 | 상담 이력 | 필요 | 작성완료 | 유능한 |  |
| HIS-06 | GET | `/api/consultations/{consultationId}/history` | 상담 상세 | 상담 이력 | 필요 | 작성완료 | 유능한 |  |
| HIS-07 | GET | `/api/admin/dashboard` | 운영 대시보드 | 통계 | 필요 | 작성완료 | 유능한 |  |

---

## HIS-01 내 상담 이력 목록

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API ID | HIS-01 |
| 기능 | 내 상담 이력 목록 |
| Method | GET |
| URL | /api/v1/customers/me/consultations |
| 사용 화면 | 고객 메인(최근 상담), 내 상담 이력 |
| 호출 시점 | 화면 진입, 탭(전체·AI 응대·상담원 연결) 변경, 더 보기 클릭 시 |
| 인증 필요 | 예 (CUSTOMER) |
| 설명 | 로그인한 고객의 종료된 상담(CLOSED, TERMINATED)을 최신순으로 조회한다. 고객 메인의 최근 상담은 size=2로 호출한다. 카드를 펼칠 때의 대화 미리보기는 HIS-06으로 받는다. (REQ-031) |

### Request Header

| 이름 | 값 | 필수 | 설명 |
| --- | --- | --- | --- |
| Authorization | Bearer {accessToken} | Y | JWT 인증 |

### Path Variable

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| 없음 |  |  |  |

### Query Parameter

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| type | String | N | ALL(기본), AI, AGENT |
| page | Integer | N | 페이지 번호, 기본 0 |
| size | Integer | N | 페이지 크기 1~50, 기본 20 |

### Request Body

없음

### Response Body

```json
{
  "success": true,
  "message": "조회되었습니다.",
  "data": {
    "content": [
      {
        "consultationId": 123,
        "consultationNo": "C-000123",
        "startedAt": "2026-11-12T14:20:00",
        "closedAt": "2026-11-12T14:31:00",
        "handledBy": "AGENT",
        "status": "TERMINATED",
        "inquirySummary": "수도요금 납부 방법이 어떻게 됩니까?",
        "summary": {
          "inquiryCategory": "상하수도",
          "processSummary": "AI 응대 → 상담원 연결 요청 → 상담원 상담 → 반복 폭언으로 자동 종료 (폭언 감지 2회)",
          "result": "TERMINATED"
        },
        "violationRecorded": true
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 3,
    "totalPages": 1,
    "hasNext": false
  },
  "errorCode": null
}
```

- violationRecorded가 true이면 카드에 "상담 중 부적절한 표현으로 종료됨 · 위반 1회 누적"을 표시한다.
- 결과가 없으면 content는 빈 배열이고, 화면은 "아직 상담 이력이 없어요."를 표시한다.

### Status Code

| 상태 코드 | 설명 |
| --- | --- |
| 200 OK | 조회 성공 |
| 400 Bad Request | 파라미터 값 오류 |
| 401 Unauthorized | 로그인하지 않음 |
| 403 Forbidden | 고객이 아닌 역할 |

### 예외 상황

| 상황 | 응답 코드 | 메시지 |
| --- | --- | --- |
| 로그인하지 않은 사용자 | 401 Unauthorized (AUTH_REQUIRED) | 로그인이 필요합니다. |
| 상담원·관리자가 호출 | 403 Forbidden (ACCESS_DENIED) | 접근 권한이 없습니다. |
| type·page·size 값 오류 | 400 Bad Request (INVALID_PARAMETER) | 입력 값을 확인해주세요. |

---

## HIS-02 내 이용 상태(제재) 조회

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API ID | HIS-02 |
| 기능 | 내 이용 상태(제재) 조회 |
| Method | GET |
| URL | /api/v1/customers/me/sanction |
| 사용 화면 | 고객 메인(내 이용 상태), 이용정지 안내, 상담 종료 결과 |
| 호출 시점 | 고객 메인·이용정지 안내 화면 진입, 상태 다시 확인 클릭 시 |
| 인증 필요 | 예 (CUSTOMER, 정지 고객 포함) |
| 설명 | 현재 이용정지 여부, 누적 위반 횟수, 현재 제재 정보, 제재 기준 안내를 조회한다. 정지 여부는 released_at IS NULL AND end_at > 현재인 제재가 있는지로 판단한다. 표시용이며, 상담 시작 차단은 상담 시작 API에서 서버가 다시 검사한다. (REQ-031, REQ-022) |

### Request Header

| 이름 | 값 | 필수 | 설명 |
| --- | --- | --- | --- |
| Authorization | Bearer {accessToken} | Y | JWT 인증 |

### Path Variable

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| 없음 |  |  |  |

### Query Parameter

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| 없음 |  |  |  |

### Request Body

없음

### Response Body

```json
{
  "success": true,
  "message": "조회되었습니다.",
  "data": {
    "sanctionStatus": "SUSPENDED",
    "canStartConsultation": false,
    "totalViolationCount": 1,
    "currentSanction": {
      "sanctionId": 15,
      "suspendDays": 7,
      "startAt": "2026-11-12T14:31:00",
      "endAt": "2026-11-19T14:31:00",
      "remainingDays": 7,
      "reason": "상담 중 반복 폭언 (자동 종료)"
    },
    "policyGuide": [
      { "violationCount": 1, "suspendDays": 7, "orMore": false },
      { "violationCount": 2, "suspendDays": 30, "orMore": false },
      { "violationCount": 3, "suspendDays": 90, "orMore": true }
    ]
  },
  "errorCode": null
}
```

- 정지 중이 아니면 sanctionStatus는 NONE, canStartConsultation은 true, currentSanction은 null이다.
- remainingDays는 해제 일시까지 남은 일수를 올림한 값으로, 화면의 D-N에 쓴다.
- reason은 위반 상담의 close_reason으로 만든다. ABUSE_AUTO는 "상담 중 반복 폭언 (자동 종료)", ABUSE_MANUAL은 "상담 중 폭언 (상담원 종료)".
- policyGuide는 가장 최근 제재 정책 버전의 값이다.

### Status Code

| 상태 코드 | 설명 |
| --- | --- |
| 200 OK | 조회 성공 |
| 401 Unauthorized | 로그인하지 않음 |
| 403 Forbidden | 고객이 아닌 역할 |

### 예외 상황

| 상황 | 응답 코드 | 메시지 |
| --- | --- | --- |
| 로그인하지 않은 사용자 | 401 Unauthorized (AUTH_REQUIRED) | 로그인이 필요합니다. |
| 상담원·관리자가 호출 | 403 Forbidden (ACCESS_DENIED) | 접근 권한이 없습니다. |

---

## HIS-03 응대 고객별 이력

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API ID | HIS-03 |
| 기능 | 응대 고객별 이력 |
| Method | GET |
| URL | /api/v1/agents/me/customers |
| 사용 화면 | 상담원 내 상담 이력 |
| 호출 시점 | 화면 진입, 조회 버튼 클릭, 페이지 이동 시 |
| 인증 필요 | 예 (AGENT) |
| 설명 | 상단 카드 4개(응대한 고객, 총 상담 건수, 평균 상담 시간, 폭언 자동 종료)와 고객별 목록을 함께 조회한다. 한 행은 고객 1명이고, 값은 그 고객과 나의 최근 상담 기준이다. agent_id가 본인인 종료 상담만 대상이다. (REQ-030, NFR-004) |

### Request Header

| 이름 | 값 | 필수 | 설명 |
| --- | --- | --- | --- |
| Authorization | Bearer {accessToken} | Y | JWT 인증 |

### Path Variable

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| 없음 |  |  |  |

### Query Parameter

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| from | Date | N | 조회 시작일(상담 시작일 기준), 예 2026-11-01 |
| to | Date | N | 조회 종료일 |
| status | String | N | 최근 상태: ALL(기본), CLOSED, TERMINATED |
| keyword | String | N | 고객 아이디 또는 상담번호(C-000123, 123 모두 허용) |
| page | Integer | N | 기본 0 |
| size | Integer | N | 기본 20 |

### Request Body

없음

### Response Body

```json
{
  "success": true,
  "message": "조회되었습니다.",
  "data": {
    "stats": {
      "servedCustomerCount": 42,
      "totalConsultationCount": 57,
      "avgDurationSeconds": 412,
      "abuseTerminatedCount": 3
    },
    "customers": {
      "content": [
        {
          "customerId": 7,
          "customerLoginId": "user0007",
          "hasViolationHistory": true,
          "myConsultationCount": 2,
          "latestConsultation": {
            "consultationId": 123,
            "consultationNo": "C-000123",
            "startedAt": "2026-11-12T14:20:00",
            "inquirySummary": "수도요금 납부 방법이 어떻게 됩니까?",
            "abuseCount": 2,
            "status": "TERMINATED"
          }
        }
      ],
      "page": 0,
      "size": 20,
      "totalElements": 42,
      "totalPages": 3,
      "hasNext": true
    }
  },
  "errorCode": null
}
```

- stats는 같은 기간 필터를 따른다. 평균 상담 시간은 accepted_at부터 closed_at까지, 폭언 자동 종료는 내가 맡은 TERMINATED 상담 수다.
- hasViolationHistory는 고객의 위반이 1건 이상이면 true다(위반 이력 배지). 폭언 원문은 내려주지 않는다.
- abuseCount는 최근 상담의 폭언 원문(ABUSE_LOG) 건수다.
- 정렬: 최근 상담 일시 내림차순.

### Status Code

| 상태 코드 | 설명 |
| --- | --- |
| 200 OK | 조회 성공 |
| 400 Bad Request | 기간·파라미터 오류 |
| 401 Unauthorized | 로그인하지 않음 |
| 403 Forbidden | 상담원이 아닌 역할 |

### 예외 상황

| 상황 | 응답 코드 | 메시지 |
| --- | --- | --- |
| 로그인하지 않은 사용자 | 401 Unauthorized (AUTH_REQUIRED) | 로그인이 필요합니다. |
| 고객·관리자가 호출 | 403 Forbidden (ACCESS_DENIED) | 접근 권한이 없습니다. |
| 시작일이 종료일보다 늦거나 기간이 366일 초과 | 400 Bad Request (INVALID_PERIOD) | 조회 기간을 다시 선택해주세요. |

---

## HIS-04 내가 맡은 상담 목록

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API ID | HIS-04 |
| 기능 | 내가 맡은 상담 목록 |
| Method | GET |
| URL | /api/v1/agents/me/consultations |
| 사용 화면 | 상담원 내 상담 이력(고객 행 선택 시) |
| 호출 시점 | HIS-03 목록에서 고객 행을 클릭했을 때 |
| 인증 필요 | 예 (AGENT) |
| 설명 | 선택한 고객과 내가 한 상담을 상담 단위로 조회한다. 각 행의 상세는 HIS-06으로 연다. 다른 상담원이 맡았던 상담은 나오지 않는다. (REQ-030, NFR-004) |

### Request Header

| 이름 | 값 | 필수 | 설명 |
| --- | --- | --- | --- |
| Authorization | Bearer {accessToken} | Y | JWT 인증 |

### Path Variable

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| 없음 |  |  |  |

### Query Parameter

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| customerId | Long | N | 고객 회원 ID. 없으면 내가 맡은 상담 전체 |
| from | Date | N | 상담 시작일 기준 |
| to | Date | N | 상담 시작일 기준 |
| status | String | N | ALL(기본), CLOSED, TERMINATED |
| page | Integer | N | 기본 0 |
| size | Integer | N | 기본 20 |

### Request Body

없음

### Response Body

```json
{
  "success": true,
  "message": "조회되었습니다.",
  "data": {
    "content": [
      {
        "consultationId": 123,
        "consultationNo": "C-000123",
        "customerLoginId": "user0007",
        "startedAt": "2026-11-12T14:20:00",
        "closedAt": "2026-11-12T14:31:00",
        "inquirySummary": "수도요금 납부 방법이 어떻게 됩니까?",
        "abuseCount": 2,
        "status": "TERMINATED"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 2,
    "totalPages": 1,
    "hasNext": false
  },
  "errorCode": null
}
```

### Status Code

| 상태 코드 | 설명 |
| --- | --- |
| 200 OK | 조회 성공 |
| 400 Bad Request | 기간·파라미터 오류 |
| 401 Unauthorized | 로그인하지 않음 |
| 403 Forbidden | 상담원이 아닌 역할 |

### 예외 상황

| 상황 | 응답 코드 | 메시지 |
| --- | --- | --- |
| 로그인하지 않은 사용자 | 401 Unauthorized (AUTH_REQUIRED) | 로그인이 필요합니다. |
| 고객·관리자가 호출 | 403 Forbidden (ACCESS_DENIED) | 접근 권한이 없습니다. |
| 기간 오류 | 400 Bad Request (INVALID_PERIOD) | 조회 기간을 다시 선택해주세요. |

---

## HIS-05 전체 상담 이력

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API ID | HIS-05 |
| 기능 | 전체 상담 이력 |
| Method | GET |
| URL | /api/v1/admin/consultations |
| 사용 화면 | 관리자 상담 이력 |
| 호출 시점 | 화면 진입, 조회 버튼 클릭, 페이지 이동 시 |
| 인증 필요 | 예 (ADMIN) |
| 설명 | 종료된 전체 상담을 기간·상태·상담원·상담번호·고객 아이디로 검색한다. 폭언 원문은 포함하지 않는다. (REQ-030, NFR-012) |

### Request Header

| 이름 | 값 | 필수 | 설명 |
| --- | --- | --- | --- |
| Authorization | Bearer {accessToken} | Y | JWT 인증 |

### Path Variable

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| 없음 |  |  |  |

### Query Parameter

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| from | Date | N | 상담 시작일 기준 |
| to | Date | N | 상담 시작일 기준 |
| status | String | N | ALL(기본), CLOSED, TERMINATED |
| handledBy | String | N | ALL(기본), AI, AGENT |
| agentId | Long | N | 상담원 회원 ID |
| keyword | String | N | 상담번호 또는 고객 아이디 |
| page | Integer | N | 기본 0 |
| size | Integer | N | 기본 20 |

### Request Body

없음

### Response Body

```json
{
  "success": true,
  "message": "조회되었습니다.",
  "data": {
    "content": [
      {
        "consultationId": 123,
        "consultationNo": "C-000123",
        "startedAt": "2026-11-12T14:20:00",
        "customerId": 7,
        "customerLoginId": "user0007",
        "agentId": 3,
        "agentName": "김상담",
        "handledBy": "AGENT",
        "abuseCount": 2,
        "status": "TERMINATED"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 120,
    "totalPages": 6,
    "hasNext": true
  },
  "errorCode": null
}
```

- AI만 응대한 상담은 agentId, agentName이 null이고 화면은 "—"로 표시한다.
- 정렬: 상담 시작 일시 내림차순.
- 상담원 필터 드롭다운의 상담원 목록은 회원 API(장건웅)를 사용한다.

### Status Code

| 상태 코드 | 설명 |
| --- | --- |
| 200 OK | 조회 성공 |
| 400 Bad Request | 기간·파라미터 오류 |
| 401 Unauthorized | 로그인하지 않음 |
| 403 Forbidden | 관리자가 아닌 역할 |

### 예외 상황

| 상황 | 응답 코드 | 메시지 |
| --- | --- | --- |
| 로그인하지 않은 사용자 | 401 Unauthorized (AUTH_REQUIRED) | 로그인이 필요합니다. |
| 고객·상담원이 호출 | 403 Forbidden (ACCESS_DENIED) | 접근 권한이 없습니다. |
| 기간 오류 | 400 Bad Request (INVALID_PERIOD) | 조회 기간을 다시 선택해주세요. |
| 결과 없음 | 200 OK (content 빈 배열) | 조건에 맞는 상담 이력이 없습니다. |

---

## HIS-06 상담 상세

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API ID | HIS-06 |
| 기능 | 상담 상세 |
| Method | GET |
| URL | /api/v1/consultations/{consultationId}/history |
| 사용 화면 | 상담 상세(고객·상담원·관리자), 고객 내 상담 이력 카드 펼치기 |
| 호출 시점 | 이력 목록에서 상세 클릭 또는 카드 펼칠 때 |
| 인증 필요 | 예 (CUSTOMER, AGENT, ADMIN) |
| 설명 | 종료된 상담 1건의 기본 정보, 요약, 전체 메시지를 읽기 전용으로 조회한다. 세 역할이 같은 URL을 쓰고, 조회 범위와 메시지 내용은 서버가 역할에 따라 정한다. (REQ-030, REQ-031) |

역할별 조회 규칙

| 역할 | 조회 가능 상담 | 폭언 메시지 content |
| --- | --- | --- |
| CUSTOMER | customer_id = 본인 | 본인이 보낸 원문(ABUSE_LOG 조인, 정책 6장) |
| AGENT | agent_id = 본인 | 마스킹본(MESSAGE.content) |
| ADMIN | 전체 | 마스킹본. 원문은 제재 상세에서만 열람 |

### Request Header

| 이름 | 값 | 필수 | 설명 |
| --- | --- | --- | --- |
| Authorization | Bearer {accessToken} | Y | JWT 인증 |

### Path Variable

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| consultationId | Long | Y | 상담 ID |

### Query Parameter

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| 없음 |  |  |  |

### Request Body

없음

### Response Body

```json
{
  "success": true,
  "message": "조회되었습니다.",
  "data": {
    "consultation": {
      "consultationId": 123,
      "consultationNo": "C-000123",
      "status": "TERMINATED",
      "customerLoginId": "user0007",
      "agentName": "김상담",
      "startedAt": "2026-11-12T14:20:00",
      "queuedAt": "2026-11-12T14:22:10",
      "acceptedAt": "2026-11-12T14:23:00",
      "closedAt": "2026-11-12T14:31:00",
      "closedBy": "SYSTEM",
      "closeReason": "ABUSE_AUTO",
      "abuseCount": 2,
      "violationRecorded": true
    },
    "summary": {
      "summaryStatus": "DONE",
      "inquiryCategory": "상하수도",
      "inquirySummary": "수도요금 납부 방법이 어떻게 됩니까?",
      "processSummary": "AI 응대 → 상담원 연결 요청 → 상담원 상담 → 반복 폭언으로 자동 종료 (폭언 감지 2회)",
      "result": "TERMINATED"
    },
    "messages": [
      {
        "messageId": 9001,
        "senderType": "CUSTOMER",
        "senderName": "user0007",
        "content": "수도요금 납부 방법 알려주세요",
        "abuseStatus": null,
        "warningNo": null,
        "createdAt": "2026-11-12T14:20:05.120"
      },
      {
        "messageId": 9008,
        "senderType": "CUSTOMER",
        "senderName": "user0007",
        "content": "*** 왜 이렇게 늦어요",
        "abuseStatus": "ABUSIVE",
        "warningNo": 1,
        "createdAt": "2026-11-12T14:26:41.502"
      }
    ]
  },
  "errorCode": null
}
```

- 위 예시는 상담원·관리자가 받는 형태다. 고객이 조회하면 9008번 메시지의 content가 본인이 보낸 원문으로 내려간다.
- messages는 created_at 오름차순 전체다(페이징 없음). 고객 이력 카드의 미리보기는 화면에서 앞 3개만 보여준다.
- AI 상담 구간, 상담원 연결, 상담 자동 종료 구분선은 화면이 queuedAt·acceptedAt·closedAt으로 그린다.
- warningNo는 이 상담에서 몇 번째 경고인지다(WARNING을 created_at 순으로 번호). ABUSIVE · 경고 1 배지에 쓴다.
- summaryStatus는 DONE(요약 있음) 또는 NONE(요약 실패, 요약 필드는 "요약 없음")이다. 요약이 없어도 메시지는 조회된다.
- AI 메시지의 senderName은 "AI", 시스템 메시지는 "시스템"이다.

### Status Code

| 상태 코드 | 설명 |
| --- | --- |
| 200 OK | 조회 성공 |
| 401 Unauthorized | 로그인하지 않음 |
| 403 Forbidden | 조회 권한 없는 상담 |
| 404 Not Found | 존재하지 않는 상담 |
| 409 Conflict | 진행 중인 상담 |

### 예외 상황

| 상황 | 응답 코드 | 메시지 |
| --- | --- | --- |
| 로그인하지 않은 사용자 | 401 Unauthorized (AUTH_REQUIRED) | 로그인이 필요합니다. |
| 다른 고객의 상담, 다른 상담원이 맡은 상담 | 403 Forbidden (ACCESS_DENIED) | 접근 권한이 없습니다. |
| 없는 상담 ID | 404 Not Found (CONSULTATION_NOT_FOUND) | 상담을 찾을 수 없습니다. |
| 아직 종료되지 않은 상담 | 409 Conflict (CONSULTATION_NOT_CLOSED) | 진행 중인 상담입니다. 상담 화면에서 확인해주세요. |

---

## HIS-07 운영 대시보드

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API ID | HIS-07 |
| 기능 | 운영 대시보드 |
| Method | GET |
| URL | /api/v1/admin/dashboard |
| 사용 화면 | 관리자 대시보드(운영 현황·통계) |
| 호출 시점 | 화면 진입, 기간 탭(오늘·7일·30일) 변경 시 |
| 인증 필요 | 예 (ADMIN) |
| 설명 | 요약 카드, 일별 상담·폭언 감지 추이, 상담 종료 유형, 최근 제재 3건을 한 번에 조회한다. 통계는 일 단위로 집계한다. (REQ-032) |

### Request Header

| 이름 | 값 | 필수 | 설명 |
| --- | --- | --- | --- |
| Authorization | Bearer {accessToken} | Y | JWT 인증 |

### Path Variable

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| 없음 |  |  |  |

### Query Parameter

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| period | String | N | TODAY(기본), 7D, 30D. 오늘 포함 |

### Request Body

없음

### Response Body

```json
{
  "success": true,
  "message": "조회되었습니다.",
  "data": {
    "period": { "type": "7D", "from": "2026-11-06", "to": "2026-11-12" },
    "cards": {
      "totalConsultationCount": 120,
      "aiOnlyResolvedRate": 41.7,
      "abuseDetectedCount": 18,
      "autoTerminatedCount": 6,
      "newSanctionCount": 6,
      "currentSuspendedCustomerCount": 4
    },
    "dailyTrend": [
      { "date": "2026-11-06", "consultationCount": 15, "abuseDetectedCount": 2 }
    ],
    "closeTypes": {
      "totalClosedCount": 115,
      "aiClosed": { "count": 48, "rate": 41.7 },
      "agentClosed": { "count": 55, "rate": 47.8 },
      "queueCanceled": { "count": 5, "rate": 4.3 },
      "terminated": { "count": 7, "rate": 6.1 }
    },
    "recentSanctions": [
      {
        "sanctionId": 15,
        "customerId": 7,
        "customerLoginId": "user0007",
        "violationCount": 1,
        "suspendDays": 7,
        "endAt": "2026-11-19T14:31:00",
        "status": "SUSPENDED"
      }
    ]
  },
  "errorCode": null
}
```

집계 기준

| 지표 | 기준 시각 | 계산 |
| --- | --- | --- |
| 전체 상담 | 상담 created_at | 기간 내 시작된 상담 수(모든 상태) |
| AI 단독 해결률 | 상담 closed_at | CLOSED이고 queued_at NULL ÷ 기간 내 종료 상담 수 × 100 |
| 폭언 감지 | ABUSE_LOG created_at | 메시지 기준 건수 |
| 자동 종료 | 상담 closed_at | TERMINATED이고 closed_by = SYSTEM |
| 신규 제재 | SANCTION created_at | 기간 내 생긴 이용정지 수 |
| 현재 이용정지 | 조회 시점 | released_at NULL이고 end_at > 현재인 고객 수(기간 무관) |
| 종료 유형 | 상담 closed_at | AI 종료: CLOSED·queued_at NULL / 상담원 종료: CLOSED·accepted_at 있음 / 대기 취소: CLOSED·queued_at 있음·accepted_at NULL / 강제 종료: TERMINATED |

- dailyTrend는 건수가 0인 날도 포함해 TODAY 1개, 7D 7개, 30D 30개를 돌려준다.
- 비율은 소수점 한 자리다. 분모가 0이면 null이고 화면은 "집계할 데이터가 없습니다."를 표시한다.
- recentSanctions는 최근 생긴 제재 3건이다. status는 SUSPENDED(정지 중), EXPIRED(만료), RELEASED(해제됨). 전체 보기는 제재 관리 화면(장건웅)으로 이동한다.

### Status Code

| 상태 코드 | 설명 |
| --- | --- |
| 200 OK | 조회 성공 |
| 400 Bad Request | period 값 오류 |
| 401 Unauthorized | 로그인하지 않음 |
| 403 Forbidden | 관리자가 아닌 역할 |

### 예외 상황

| 상황 | 응답 코드 | 메시지 |
| --- | --- | --- |
| 로그인하지 않은 사용자 | 401 Unauthorized (AUTH_REQUIRED) | 로그인이 필요합니다. |
| 고객·상담원이 호출 | 403 Forbidden (ACCESS_DENIED) | 접근 권한이 없습니다. |
| period 값 오류 | 400 Bad Request (INVALID_PARAMETER) | 입력 값을 확인해주세요. |
| 집계 데이터 없음 | 200 OK (0건, 비율 null) | 집계할 데이터가 없습니다. |
