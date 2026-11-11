# API 명세서 — FAQ

> 출처: Notion 「API 명세서」 하위 행 API-ADM-08 ~ API-ADM-11 (마지막 수정: 2026-11-11)

[← API 명세서 목차](../api-spec.md)

| API ID | Method | URL | 기능명 | 인증 | 상태 | 담당자 | 비고 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| API-ADM-08 | GET | `/api/admin/faqs` | FAQ 목록 조회 | 필요 (ADMIN) | 작성완료 | 장건웅 |  |
| API-ADM-09 | POST | `/api/admin/faqs` | FAQ 등록 | 필요 (ADMIN) | 작성완료 | 장건웅 | could |
| API-ADM-10 | PUT | `/api/admin/faqs/{faqId}` | FAQ 수정 | 필요 (ADMIN) | 작성완료 | 장건웅 | could |
| API-ADM-11 | DELETE | `/api/admin/faqs/{faqId}` | FAQ 삭제 | 필요 (ADMIN) | 작성완료 | 장건웅 | could |

---

## API-ADM-08 FAQ 목록 조회

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API ID | API-ADM-08 |
| 기능 | FAQ 목록 조회 |
| Method | GET |
| URL | /api/admin/faqs |
| 사용 화면 | FAQ 관리 (화면 미정) |
| 호출 시점 | 화면 진입, 검색, 페이지 이동 시 |
| 인증 필요 | 예 (ADMIN) |
| 관련 요구사항 | REQ-027 (Could) |
| 설명 | Mock AI 응대에 쓰는 FAQ를 최근 수정 순으로 20건씩 준다. |

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
| keyword | String | N | 질문·답변 부분 일치 검색 |
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
        "faqId": 3,
        "category": "환불",
        "question": "환불은 언제 처리되나요?",
        "answer": "환불은 접수 후 영업일 기준 3~5일 안에 처리됩니다.",
        "keywords": ["환불", "취소"],
        "updatedAt": "2026-11-08T14:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
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

---

## API-ADM-09 FAQ 등록

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API ID | API-ADM-09 |
| 기능 | FAQ 등록 |
| Method | POST |
| URL | /api/admin/faqs |
| 사용 화면 | FAQ 관리 (화면 미정) |
| 호출 시점 | 등록 버튼 클릭 시 |
| 인증 필요 | 예 (ADMIN) |
| 관련 요구사항 | REQ-027 (Could) |
| 설명 | FAQ 한 건을 만든다. Mock AI 응대와 FAQ 키워드 매칭(심소민 담당), 상담 요약의 FAQ 카테고리(유능한 담당)가 이 데이터를 읽는다. |

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
  "category": "환불",
  "question": "환불은 언제 처리되나요?",
  "answer": "환불은 접수 후 영업일 기준 3~5일 안에 처리됩니다.",
  "keywords": ["환불", "취소"]
}
```

| 필드 | 타입 | 필수 | 검증 규칙 |
| --- | --- | --- | --- |
| category | String | Y | 1~30자 |
| question | String | Y | 1~200자 |
| answer | String | Y | 1~2000자 |
| keywords | String[] | Y | 1~10개, 각 1~20자 |

### Response Body

```json
{
  "success": true,
  "message": "FAQ가 등록되었습니다.",
  "data": {
    "faqId": 4,
    "category": "환불",
    "question": "환불은 언제 처리되나요?",
    "answer": "환불은 접수 후 영업일 기준 3~5일 안에 처리됩니다.",
    "keywords": ["환불", "취소"],
    "updatedAt": "2026-11-10T12:00:00"
  }
}
```

### Status Code

| 상태 코드 | 설명 |
| --- | --- |
| 201 Created | 등록 성공 |
| 400 Bad Request | 입력 값 검증 실패 |
| 401 Unauthorized | 비로그인 |
| 403 Forbidden | ADMIN 아님 |

### 예외 상황

| 상황 | 응답 코드 | 메시지 |
| --- | --- | --- |
| 필수 값 누락·길이 초과 | 400 Bad Request | 입력 값을 확인해주세요. |
| 비로그인 | 401 Unauthorized | 로그인이 필요합니다. |
| ADMIN 아님 | 403 Forbidden | 접근 권한이 없습니다. |

---

## API-ADM-10 FAQ 수정

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API ID | API-ADM-10 |
| 기능 | FAQ 수정 |
| Method | PUT |
| URL | /api/admin/faqs/{faqId} |
| 사용 화면 | FAQ 관리 (화면 미정) |
| 호출 시점 | 수정 후 저장 버튼 클릭 시 |
| 인증 필요 | 예 (ADMIN) |
| 관련 요구사항 | REQ-027 (Could) |
| 설명 | FAQ 전체를 바꾼다. 검증 규칙은 API-ADM-09와 같다. |

### Request Header

| 이름 | 값 | 필수 | 설명 |
| --- | --- | --- | --- |
| Authorization | Bearer {accessToken} | Y | ADMIN 계정 JWT |
| Content-Type | application/json | Y | JSON 형식 요청 |

### Path Variable

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| faqId | Long | Y | FAQ ID |

### Query Parameter

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| 없음 |  |  |  |

### Request Body

```json
{
  "category": "환불",
  "question": "환불은 언제 처리되나요?",
  "answer": "환불은 접수 후 영업일 기준 3일 안에 처리됩니다.",
  "keywords": ["환불", "취소", "반품"]
}
```

### Response Body

```json
{
  "success": true,
  "message": "FAQ가 수정되었습니다.",
  "data": {
    "faqId": 3,
    "category": "환불",
    "question": "환불은 언제 처리되나요?",
    "answer": "환불은 접수 후 영업일 기준 3일 안에 처리됩니다.",
    "keywords": ["환불", "취소", "반품"],
    "updatedAt": "2026-11-10T12:10:00"
  }
}
```

### Status Code

| 상태 코드 | 설명 |
| --- | --- |
| 200 OK | 수정 성공 |
| 400 Bad Request | 입력 값 검증 실패 |
| 401 Unauthorized | 비로그인 |
| 403 Forbidden | ADMIN 아님 |
| 404 Not Found | 없는 FAQ |

### 예외 상황

| 상황 | 응답 코드 | 메시지 |
| --- | --- | --- |
| 필수 값 누락·길이 초과 | 400 Bad Request | 입력 값을 확인해주세요. |
| 없는 FAQ | 404 Not Found | FAQ를 찾을 수 없습니다. |
| 비로그인 | 401 Unauthorized | 로그인이 필요합니다. |
| ADMIN 아님 | 403 Forbidden | 접근 권한이 없습니다. |

---

## API-ADM-11 FAQ 삭제

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API ID | API-ADM-11 |
| 기능 | FAQ 삭제 |
| Method | DELETE |
| URL | /api/admin/faqs/{faqId} |
| 사용 화면 | FAQ 관리 (화면 미정) |
| 호출 시점 | 삭제 버튼 클릭 후 확인 시 |
| 인증 필요 | 예 (ADMIN) |
| 관련 요구사항 | REQ-027 (Could) |
| 설명 | FAQ를 삭제한다. 이후 Mock AI 응대와 키워드 매칭에서 쓰지 않는다. |

### Request Header

| 이름 | 값 | 필수 | 설명 |
| --- | --- | --- | --- |
| Authorization | Bearer {accessToken} | Y | ADMIN 계정 JWT |

### Path Variable

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| faqId | Long | Y | FAQ ID |

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
  "message": "FAQ가 삭제되었습니다.",
  "data": null
}
```

### Status Code

| 상태 코드 | 설명 |
| --- | --- |
| 200 OK | 삭제 성공 |
| 401 Unauthorized | 비로그인 |
| 403 Forbidden | ADMIN 아님 |
| 404 Not Found | 없는 FAQ |

### 예외 상황

| 상황 | 응답 코드 | 메시지 |
| --- | --- | --- |
| 없는 FAQ | 404 Not Found | FAQ를 찾을 수 없습니다. |
| 비로그인 | 401 Unauthorized | 로그인이 필요합니다. |
| ADMIN 아님 | 403 Forbidden | 접근 권한이 없습니다. |
