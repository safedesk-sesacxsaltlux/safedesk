# API 명세서 — 정책

> 출처: Notion 「API 명세서」 하위 행 API-ADM-01 ~ API-ADM-03 (마지막 수정: 2026-11-10)

[← API 명세서 목차](../api-spec.md)

| API ID | Method | URL | 기능명 | 인증 | 상태 | 담당자 |
| --- | --- | --- | --- | --- | --- | --- |
| API-ADM-01 | GET | `/api/admin/policies` | 제재 정책 조회 | 필요 (ADMIN) | 작성완료 | 장건웅 |
| API-ADM-02 | PUT | `/api/admin/policies` | 제재 정책 수정 | 필요 (ADMIN) | 작성완료 | 장건웅 |
| API-ADM-03 | GET | `/api/admin/policies/histories` | 정책 변경 이력 조회 | 필요 (ADMIN) | 작성완료 | 장건웅 |

---

## API-ADM-01 제재 정책 조회

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API ID | API-ADM-01 |
| 기능 | 제재 정책 조회 |
| Method | GET |
| URL | /api/admin/policies |
| 사용 화면 | SCR-015 정책 관리 화면 |
| 호출 시점 | 화면 진입 시, 저장 성공 후 |
| 인증 필요 | 예 (ADMIN) |
| 관련 요구사항 | REQ-024 |
| 설명 | 현재 적용 중인 정책 버전과 기본값을 함께 준다. 취소·기본값 복원은 프론트가 응답값으로 폼을 다시 채운다. |

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
| 없음 |  |  |  |

### Request Body

없음

### Response Body

```json
{
  "success": true,
  "message": "요청이 처리되었습니다.",
  "data": {
    "current": {
      "policyVersion": 3,
      "warningThreshold": 1,
      "autoCloseThreshold": 2,
      "suspensionDays": { "first": 7, "second": 30, "thirdOrMore": 90 },
      "updatedAt": "2026-11-05T09:00:00",
      "updatedBy": { "memberId": 100, "name": "관리자" }
    },
    "defaults": {
      "warningThreshold": 1,
      "autoCloseThreshold": 2,
      "suspensionDays": { "first": 7, "second": 30, "thirdOrMore": 90 }
    }
  }
}
```

| 필드 | 의미 |
| --- | --- |
| policyVersion | 정책 버전. 저장할 때마다 1씩 오른다 |
| warningThreshold | 같은 상담에서 폭언 몇 회째에 경고를 보내는지 (경고 발송 기준) |
| autoCloseThreshold | 같은 상담에서 폭언 몇 회째에 자동 종료하는지 (자동 종료 기준) |
| suspensionDays.first / second / thirdOrMore | 누적 위반 1회 / 2회 / 3회 이상 이용정지 일수 |

### Status Code

| 상태 코드 | 설명 |
| --- | --- |
| 200 OK | 조회 성공 |
| 401 Unauthorized | 비로그인 |
| 403 Forbidden | ADMIN 아님 |

### 예외 상황

| 상황 | 응답 코드 | 메시지 |
| --- | --- | --- |
| 비로그인 | 401 Unauthorized | 로그인이 필요합니다. |
| ADMIN 아님 | 403 Forbidden | 접근 권한이 없습니다. |

---

## API-ADM-02 제재 정책 수정

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API ID | API-ADM-02 |
| 기능 | 제재 정책 수정 |
| Method | PUT |
| URL | /api/admin/policies |
| 사용 화면 | SCR-015 정책 관리 화면 |
| 호출 시점 | 저장 버튼 클릭 시 |
| 인증 필요 | 예 (ADMIN) |
| 관련 요구사항 | REQ-024 |
| 설명 | 정책을 새 버전으로 저장하고 변경 일시·변경한 관리자·변경 내용을 이력으로 남긴다. 바뀐 값은 저장 이후 시작하는 상담과 그 상담에서 생기는 제재에만 적용한다. 진행 중인 상담과 정지는 바꾸지 않는다. 값이 하나도 바뀌지 않았으면 새 버전을 만들지 않는다. |

### Request Header

| 이름 | 값 | 필수 | 설명 |
| --- | --- | --- | --- |
| Authorization | Bearer {accessToken} | Y | ADMIN 계정 JWT |
| Content-Type | application/json | Y | JSON 형식 요청 |

### Path Variable

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| 없음 |  |  |  |

### Query Parameter

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| 없음 |  |  |  |

### Request Body

```json
{
  "warningThreshold": 1,
  "autoCloseThreshold": 2,
  "suspensionDays": { "first": 7, "second": 30, "thirdOrMore": 90 }
}
```

| 필드 | 타입 | 필수 | 검증 규칙 |
| --- | --- | --- | --- |
| warningThreshold | Integer | Y | 1~5 |
| autoCloseThreshold | Integer | Y | 1~5, warningThreshold보다 커야 함 |
| suspensionDays.first | Integer | Y | 1~365 |
| suspensionDays.second | Integer | Y | 1~365 |
| suspensionDays.thirdOrMore | Integer | Y | 1~365 |

### Response Body

```json
{
  "success": true,
  "message": "정책이 저장되었습니다.",
  "data": {
    "policyVersion": 4,
    "warningThreshold": 1,
    "autoCloseThreshold": 2,
    "suspensionDays": { "first": 7, "second": 14, "thirdOrMore": 90 },
    "updatedAt": "2026-11-10T11:20:00",
    "updatedBy": { "memberId": 100, "name": "관리자" }
  }
}
```

### Status Code

| 상태 코드 | 설명 |
| --- | --- |
| 200 OK | 저장 성공 |
| 400 Bad Request | 입력 값 검증 실패·범위 초과 |
| 401 Unauthorized | 비로그인 |
| 403 Forbidden | ADMIN 아님 |

### 예외 상황

| 상황 | 응답 코드 | 메시지 |
| --- | --- | --- |
| 범위 초과·자동 종료 기준 ≤ 경고 기준 | 400 Bad Request | 경고 발송 기준과 자동 종료 기준은 1~5회, 정지 기간은 1~365일로 입력해주세요. 자동 종료 기준은 경고 발송 기준보다 커야 합니다. |
| 필수 값 누락·정수 아님 | 400 Bad Request | 입력 값을 확인해주세요. |
| 비로그인 | 401 Unauthorized | 로그인이 필요합니다. |
| ADMIN 아님 | 403 Forbidden | 접근 권한이 없습니다. |

---

## API-ADM-03 정책 변경 이력 조회

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API ID | API-ADM-03 |
| 기능 | 정책 변경 이력 조회 |
| Method | GET |
| URL | /api/admin/policies/histories |
| 사용 화면 | SCR-015 정책 관리 화면 (변경 이력 영역) |
| 호출 시점 | 화면 진입 시, 저장 성공 후 |
| 인증 필요 | 예 (ADMIN) |
| 관련 요구사항 | REQ-024, NFR-012 |
| 설명 | 정책 버전별 변경 내역을 최신순으로 20건씩 준다. 버전 1은 초기 데이터로 넣은 기본 정책이다. |

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
| page | Integer | N | 페이지 번호, 기본 0 |
| size | Integer | N | 페이지 크기, 기본 20 |

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
        "policyVersion": 4,
        "changedAt": "2026-11-10T11:20:00",
        "changedBy": { "memberId": 100, "name": "관리자" },
        "changes": [
          {
            "field": "suspensionDays.second",
            "fieldLabel": "누적 2회 정지 기간",
            "beforeValue": "30",
            "afterValue": "14"
          }
        ]
      },
      {
        "policyVersion": 1,
        "changedAt": "2026-11-01T09:00:00",
        "changedBy": { "memberId": 100, "name": "관리자" },
        "changes": []
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 2,
    "totalPages": 1
  }
}
```

### Status Code

| 상태 코드 | 설명 |
| --- | --- |
| 200 OK | 조회 성공 |
| 401 Unauthorized | 비로그인 |
| 403 Forbidden | ADMIN 아님 |

### 예외 상황

| 상황 | 응답 코드 | 메시지 |
| --- | --- | --- |
| 비로그인 | 401 Unauthorized | 로그인이 필요합니다. |
| ADMIN 아님 | 403 Forbidden | 접근 권한이 없습니다. |
