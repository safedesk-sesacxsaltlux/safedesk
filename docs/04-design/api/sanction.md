# API 명세서 — 제재

> 출처: Notion 「API 명세서」 하위 행 API-ADM-04 ~ API-ADM-07 (마지막 수정: 2026-11-10)

[← API 명세서 목차](../api-spec.md)

| API ID | Method | URL | 기능명 | 인증 | 상태 | 담당자 |
| --- | --- | --- | --- | --- | --- | --- |
| API-ADM-04 | GET | `/api/admin/sanctions` | 제재 목록 조회 | 필요 (ADMIN) | 작성완료 | 장건웅 |
| API-ADM-05 | GET | `/api/admin/sanctions/{sanctionId}` | 제재 상세 조회 | 필요 (ADMIN) | 작성완료 | 장건웅 |
| API-ADM-06 | POST | `/api/admin/sanctions/{sanctionId}/release` | 제재 수동 해제 | 필요 (ADMIN) | 작성완료 | 장건웅 |
| API-ADM-07 | GET | `/api/admin/violations/{violationId}/original` | 위반 원문 조회 | 필요 (ADMIN) | 작성완료 | 장건웅 |

---

## API-ADM-04 제재 목록 조회

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API ID | API-ADM-04 |
| 기능 | 제재 목록 조회 |
| Method | GET |
| URL | /api/admin/sanctions |
| 사용 화면 | SCR-013 제재 관리 화면 |
| 호출 시점 | 화면 진입, 검색·필터 조회, 페이지 이동 시 |
| 인증 필요 | 예 (ADMIN) |
| 관련 요구사항 | REQ-025, NFR-012 |
| 설명 | 제재 1건이 한 행이다. 최신순으로 20건씩 준다. 상태는 회원 상태가 아니라 제재 기록의 해제 일시와 수동 해제 여부로 서버가 계산한다. |

상태 값

| status | 화면 표시 | 판단 기준 |
| --- | --- | --- |
| SUSPENDED | 정지 중 | 수동 해제 안 됨, 현재 시각 < endsAt |
| RELEASED | 해제됨 | 관리자가 수동 해제함 |
| EXPIRED | 기간 만료 | 수동 해제 안 됨, 현재 시각 ≥ endsAt |

### Request Header

| 이름 | 값 | 필수 | 설명 |
| --- | --- | --- | --- |
| Authorization | Bearer {accessToken} | Y | ADMIN 계정 JWT |

### Path Variable

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| 없음 |  |  |  |

### Query Parameter

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| keyword | String | N | 고객 아이디 부분 일치 검색 |
| status | String | N | SUSPENDED, RELEASED, EXPIRED (없으면 전체) |
| durationDays | Integer | N | 제재 기간 필터 (예: 7, 30, 90) |
| page | Integer | N | 기본 0 |
| size | Integer | N | 기본 20 |

### Request Body

없음

### Response Body

```json
{
  "success": true,
  "message": "요청이 처리되었습니다.",
  "data": {
    "content": [
      {
        "sanctionId": 31,
        "customer": { "memberId": 1, "loginId": "user01", "name": "홍길동" },
        "reason": "상담 중 폭언",
        "violationCount": 2,
        "lastViolatedAt": "2026-11-09T15:12:00",
        "durationDays": 30,
        "startsAt": "2026-11-09T15:12:00",
        "endsAt": "2026-12-09T15:12:00",
        "status": "SUSPENDED"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 4,
    "totalPages": 1
  }
}
```

### Status Code

| 상태 코드 | 설명 |
| --- | --- |
| 200 OK | 조회 성공 (결과 0건 포함) |
| 400 Bad Request | 필터 값 오류 |
| 401 Unauthorized | 비로그인 |
| 403 Forbidden | ADMIN 아님 |

### 예외 상황

| 상황 | 응답 코드 | 메시지 |
| --- | --- | --- |
| status·durationDays 값 오류 | 400 Bad Request | 입력 값을 확인해주세요. |
| 비로그인 | 401 Unauthorized | 로그인이 필요합니다. |
| ADMIN 아님 | 403 Forbidden | 접근 권한이 없습니다. |

---

## API-ADM-05 제재 상세 조회

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API ID | API-ADM-05 |
| 기능 | 제재 상세 조회 |
| Method | GET |
| URL | /api/admin/sanctions/{sanctionId} |
| 사용 화면 | SCR-014 제재 상세 화면 |
| 호출 시점 | 제재 목록에서 상세 보기 클릭 시, 해제 성공 후 |
| 인증 필요 | 예 (ADMIN) |
| 관련 요구사항 | REQ-025, REQ-026 |
| 설명 | 고객 정보, 제재 상태·사유·기간, 그 고객의 누적 위반 내역과 관련 상담 번호를 준다. 폭언 원문은 담지 않고, 위반별로 API-ADM-07을 호출해 본다. |

### Request Header

| 이름 | 값 | 필수 | 설명 |
| --- | --- | --- | --- |
| Authorization | Bearer {accessToken} | Y | ADMIN 계정 JWT |

### Path Variable

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| sanctionId | Long | Y | 제재 ID |

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
  "message": "요청이 처리되었습니다.",
  "data": {
    "sanctionId": 31,
    "customer": {
      "memberId": 1,
      "loginId": "user01",
      "name": "홍길동",
      "joinedAt": "2026-10-01T10:00:00"
    },
    "reason": "상담 중 폭언",
    "violationCount": 2,
    "durationDays": 30,
    "startsAt": "2026-11-09T15:12:00",
    "endsAt": "2026-12-09T15:12:00",
    "status": "SUSPENDED",
    "releasable": true,
    "release": null,
    "violations": [
      {
        "violationId": 8,
        "sequence": 2,
        "consultationId": 5021,
        "closedBy": "SYSTEM",
        "violatedAt": "2026-11-09T15:12:00",
        "abusiveMessageCount": 2,
        "appliedDurationDays": 30
      },
      {
        "violationId": 3,
        "sequence": 1,
        "consultationId": 4870,
        "closedBy": "AGENT",
        "violatedAt": "2026-10-20T11:03:00",
        "abusiveMessageCount": 1,
        "appliedDurationDays": 7
      }
    ]
  }
}
```

| 필드 | 의미 |
| --- | --- |
| releasable | 해제 버튼 활성화 여부 (SUSPENDED일 때만 true) |
| release | 수동 해제된 경우 releasedAt, releasedBy(memberId, name), reason. 아니면 null |
| closedBy | SYSTEM(폭언 자동 종료) 또는 AGENT(상담원 폭언 종료) |
| consultationId | 관련 상담 링크용 (SCR-017 /admin/history/{consultationId}) |

### Status Code

| 상태 코드 | 설명 |
| --- | --- |
| 200 OK | 조회 성공 |
| 401 Unauthorized | 비로그인 |
| 403 Forbidden | ADMIN 아님 |
| 404 Not Found | 없는 제재 |

### 예외 상황

| 상황 | 응답 코드 | 메시지 |
| --- | --- | --- |
| 없는 제재 | 404 Not Found | 제재 정보를 찾을 수 없습니다. |
| 비로그인 | 401 Unauthorized | 로그인이 필요합니다. |
| ADMIN 아님 | 403 Forbidden | 접근 권한이 없습니다. |

---

## API-ADM-06 제재 수동 해제

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API ID | API-ADM-06 |
| 기능 | 제재 수동 해제 |
| Method | POST |
| URL | /api/admin/sanctions/{sanctionId}/release |
| 사용 화면 | SCR-014 제재 상세 화면 (제재 해제 확인 모달) |
| 호출 시점 | 확인 모달에서 해제 확정 시 |
| 인증 필요 | 예 (ADMIN) |
| 관련 요구사항 | REQ-025 |
| 설명 | 정지를 즉시 풀고 해제한 관리자·시각·사유를 기록한다. 해제 사유는 필수다. 위반 기록과 누적 횟수는 그대로 둔다. 고객은 바로 상담을 시작할 수 있다. |

### Request Header

| 이름 | 값 | 필수 | 설명 |
| --- | --- | --- | --- |
| Authorization | Bearer {accessToken} | Y | ADMIN 계정 JWT |
| Content-Type | application/json | Y | JSON 형식 요청 |

### Path Variable

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| sanctionId | Long | Y | 제재 ID |

### Query Parameter

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| 없음 |  |  |  |

### Request Body

```json
{
  "reason": "상담 내용 재확인 결과 오탐으로 판단"
}
```

| 필드 | 타입 | 필수 | 검증 규칙 |
| --- | --- | --- | --- |
| reason | String | Y | 공백 제외 1~500자 |

### Response Body

```json
{
  "success": true,
  "message": "제재가 해제되었습니다.",
  "data": {
    "sanctionId": 31,
    "status": "RELEASED",
    "violationCount": 2,
    "release": {
      "releasedAt": "2026-11-10T11:30:00",
      "releasedBy": { "memberId": 100, "name": "관리자" },
      "reason": "상담 내용 재확인 결과 오탐으로 판단"
    }
  }
}
```

### Status Code

| 상태 코드 | 설명 |
| --- | --- |
| 200 OK | 해제 성공 |
| 400 Bad Request | 해제 사유 누락 |
| 401 Unauthorized | 비로그인 |
| 403 Forbidden | ADMIN 아님 |
| 404 Not Found | 없는 제재 |
| 409 Conflict | 이미 해제됐거나 기간 만료 |

### 예외 상황

| 상황 | 응답 코드 | 메시지 |
| --- | --- | --- |
| 사유 누락 | 400 Bad Request | 해제 사유를 입력해주세요. |
| 없는 제재 | 404 Not Found | 제재 정보를 찾을 수 없습니다. |
| 이미 해제됨·기간 만료 | 409 Conflict | 이미 해제되었거나 기간이 끝난 제재입니다. |
| 비로그인 | 401 Unauthorized | 로그인이 필요합니다. |
| ADMIN 아님 | 403 Forbidden | 접근 권한이 없습니다. |

---

## API-ADM-07 위반 원문 조회

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API ID | API-ADM-07 |
| 기능 | 위반 원문 조회 |
| Method | GET |
| URL | /api/admin/violations/{violationId}/original |
| 사용 화면 | SCR-014 제재 상세 화면 (폭언 원문 확인 모달) |
| 호출 시점 | 폭언 원문 확인 버튼 클릭 시 |
| 인증 필요 | 예 (ADMIN) |
| 관련 요구사항 | REQ-026, NFR-005 |
| 설명 | 위반 1건에 속한 폭언 메시지의 원문과 판정 결과를 준다. 요청할 때마다 서버가 관리자 권한을 확인하고 열람 기록(관리자 ID, 시각, violationId)을 남긴다. 원문은 조회만 되고 수정·삭제 API는 두지 않는다. |

### Request Header

| 이름 | 값 | 필수 | 설명 |
| --- | --- | --- | --- |
| Authorization | Bearer {accessToken} | Y | ADMIN 계정 JWT |

### Path Variable

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| violationId | Long | Y | 위반 ID (API-ADM-05의 violations에서 받음) |

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
  "message": "요청이 처리되었습니다.",
  "data": {
    "violationId": 8,
    "consultationId": 5021,
    "customerLoginId": "user01",
    "closedBy": "SYSTEM",
    "violatedAt": "2026-11-09T15:12:00",
    "messages": [
      {
        "messageId": 90211,
        "sentAt": "2026-11-09T15:10:00",
        "classification": "ABUSIVE",
        "detectedBy": "KEYWORD",
        "warningOrder": 1,
        "originalContent": "(폭언 원문)",
        "maskedContent": "*** 진짜 왜 이렇게 처리해요"
      },
      {
        "messageId": 90214,
        "sentAt": "2026-11-09T15:12:00",
        "classification": "ABUSIVE",
        "detectedBy": "KEYWORD",
        "warningOrder": 2,
        "originalContent": "(폭언 원문)",
        "maskedContent": "*** 그냥 끊어요"
      }
    ],
    "viewedAt": "2026-11-10T11:25:00"
  }
}
```

classification은 정책상 정상/폭언 두 가지라 여기에는 항상 ABUSIVE만 나온다. detectedBy는 KEYWORD(금칙어) 또는 AGENT(상담원 수동 경고)다.

### Status Code

| 상태 코드 | 설명 |
| --- | --- |
| 200 OK | 조회 성공 |
| 401 Unauthorized | 비로그인 |
| 403 Forbidden | ADMIN 아님 (상담원 포함) |
| 404 Not Found | 없는 위반 |

### 예외 상황

| 상황 | 응답 코드 | 메시지 |
| --- | --- | --- |
| 없는 위반 | 404 Not Found | 위반 정보를 찾을 수 없습니다. |
| 비로그인 | 401 Unauthorized | 로그인이 필요합니다. |
| ADMIN 아님 | 403 Forbidden | 접근 권한이 없습니다. |
