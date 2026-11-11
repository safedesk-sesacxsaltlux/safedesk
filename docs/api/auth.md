# API 명세서 — 회원·인증

[← API 명세서 목차](README.md)

| API ID | Method | URL | 기능명 | 분류 | 인증 | 상태 | 담당자 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| API-AUTH-01 | POST | `/api/auth/signup` | 회원가입 | 회원 | 불필요 | 작성완료 | 장건웅 |
| API-AUTH-02 | GET | `/api/auth/check-login-id` | 아이디 중복 확인 | 회원 | 불필요 | 작성완료 | 장건웅 |
| API-AUTH-03 | POST | `/api/auth/login` | 로그인 | 인증 | 불필요 | 작성완료 | 장건웅 |
| API-AUTH-04 | POST | `/api/auth/logout` | 로그아웃 | 인증 | 필요 | 작성완료 | 장건웅 |
| API-AUTH-05 | GET | `/api/auth/me` | 내 정보 조회 | 인증 | 필요 | 작성완료 | 장건웅 |

---

## API-AUTH-01 회원가입

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API ID | API-AUTH-01 |
| 기능 | 회원가입 |
| Method | POST |
| URL | /api/auth/signup |
| 사용 화면 | SCR-002 회원가입 페이지 |
| 호출 시점 | 가입하기 버튼 클릭 시 |
| 인증 필요 | 아니오 |
| 관련 요구사항 | REQ-001, NFR-001 |
| 설명 | 고객(CUSTOMER) 계정만 만든다. 역할은 요청으로 받지 않고 서버가 CUSTOMER로 고정한다. 상담원·관리자는 초기 데이터로만 등록한다. 비밀번호는 BCrypt로 단방향 암호화해 저장한다. 상담원 보호 정책 동의(policyAgreed)는 서버에서 true인지 확인만 하고 DB에는 저장하지 않는다. |

### Request Header

| 이름 | 값 | 필수 | 설명 |
| --- | --- | --- | --- |
| Content-Type | application/json | Y | JSON 형식 요청 |

### Request Body

```json
{
  "loginId": "user01",
  "password": "pass1234!",
  "passwordConfirm": "pass1234!",
  "name": "홍길동",
  "policyAgreed": true
}
```

| 필드 | 타입 | 필수 | 검증 규칙 |
| --- | --- | --- | --- |
| loginId | String | Y | 영문 소문자·숫자 4~20자, 중복 불가 |
| password | String | Y | 8~20자, 영문·숫자·특수문자 포함 |
| passwordConfirm | String | Y | password와 일치 |
| name | String | Y | 1~20자 |
| policyAgreed | Boolean | Y | true여야 함 (저장하지 않음) |

### Response Body

```json
{
  "success": true,
  "message": "회원가입이 완료되었습니다.",
  "data": {
    "memberId": 1,
    "loginId": "user01",
    "name": "홍길동",
    "role": "CUSTOMER",
    "createdAt": "2026-11-10T10:00:00"
  }
}
```

### Status Code

| 상태 코드 | 설명 |
| --- | --- |
| 201 Created | 회원가입 성공 |
| 400 Bad Request | 입력 값 검증 실패·정책 미동의 |
| 409 Conflict | 아이디 중복 |

### 예외 상황

| 상황 | 응답 코드 | 메시지 |
| --- | --- | --- |
| 필수 값 누락·형식 오류 | 400 Bad Request | 입력 값을 확인해주세요. |
| 비밀번호 확인 불일치 | 400 Bad Request | 비밀번호가 일치하지 않습니다. |
| 정책 미동의 | 400 Bad Request | 상담원 보호 정책에 동의해주세요. |
| 아이디 중복 | 409 Conflict | 이미 사용 중인 아이디입니다. |

---

## API-AUTH-02 아이디 중복 확인

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API ID | API-AUTH-02 |
| 기능 | 아이디 중복 확인 |
| Method | GET |
| URL | /api/auth/check-login-id |
| 사용 화면 | SCR-002 회원가입 페이지 |
| 호출 시점 | 중복 확인 버튼 클릭 시 |
| 인증 필요 | 아니오 |
| 관련 요구사항 | REQ-001 |
| 설명 | 가입 전 아이디 사용 가능 여부를 알려준다. 가입 시(API-AUTH-01)에도 서버가 다시 검사한다. |

### Query Parameter

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| loginId | String | Y | 확인할 아이디 |

### Response Body

```json
{
  "success": true,
  "message": "사용 가능한 아이디입니다.",
  "data": {
    "loginId": "user01",
    "available": true
  }
}
```

### Status Code

| 상태 코드 | 설명 |
| --- | --- |
| 200 OK | 확인 성공 (사용 가능 여부는 available로 구분) |
| 400 Bad Request | 아이디 형식 오류 |

### 예외 상황

| 상황 | 응답 코드 | 메시지 |
| --- | --- | --- |
| 이미 사용 중 | 200 OK (available: false) | 이미 사용 중인 아이디입니다. |
| 형식 오류·값 누락 | 400 Bad Request | 입력 값을 확인해주세요. |

---

## API-AUTH-03 로그인

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API ID | API-AUTH-03 |
| 기능 | 로그인 |
| Method | POST |
| URL | /api/auth/login |
| 사용 화면 | SCR-003 로그인 페이지 |
| 호출 시점 | 로그인 버튼 클릭 시 |
| 인증 필요 | 아니오 |
| 관련 요구사항 | REQ-002, NFR-002 |
| 설명 | 아이디·비밀번호가 맞고 회원 상태가 ACTIVE면 JWT를 발급한다. 프론트는 role로 첫 화면을 정한다 (CUSTOMER → /customer, AGENT → /agent, ADMIN → /admin). 이용정지 고객도 로그인은 된다. |

### Request Header

| 이름 | 값 | 필수 | 설명 |
| --- | --- | --- | --- |
| Content-Type | application/json | Y | JSON 형식 요청 |

### Request Body

```json
{
  "loginId": "user01",
  "password": "pass1234!"
}
```

### Response Body

```json
{
  "success": true,
  "message": "로그인되었습니다.",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "member": {
      "memberId": 1,
      "loginId": "user01",
      "name": "홍길동",
      "role": "CUSTOMER"
    }
  }
}
```

JWT payload에는 sub(memberId), role, exp만 넣는다. 같은 토큰을 WebSocket 연결 인증에도 쓴다.

### Status Code

| 상태 코드 | 설명 |
| --- | --- |
| 200 OK | 로그인 성공 |
| 400 Bad Request | 입력 값 검증 실패 |
| 401 Unauthorized | 아이디 또는 비밀번호 불일치 |
| 403 Forbidden | INACTIVE 회원 |

### 예외 상황

| 상황 | 응답 코드 | 메시지 |
| --- | --- | --- |
| 필수 값 누락 | 400 Bad Request | 입력 값을 확인해주세요. |
| 아이디 없음·비밀번호 불일치 | 401 Unauthorized | 아이디 또는 비밀번호가 올바르지 않습니다. |
| INACTIVE 회원 | 403 Forbidden | 사용할 수 없는 계정입니다. |

---

## API-AUTH-04 로그아웃

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API ID | API-AUTH-04 |
| 기능 | 로그아웃 |
| Method | POST |
| URL | /api/auth/logout |
| 사용 화면 | 고객 메인·상담원 콘솔·관리자 대시보드의 로그아웃 버튼 |
| 호출 시점 | 로그아웃 버튼 클릭 시 |
| 인증 필요 | 예 |
| 관련 요구사항 | REQ-003 |
| 설명 | 서버는 해당 토큰을 만료 시각까지 블랙리스트에 올리고, 프론트는 저장된 토큰을 지운다. 상담원이 상담 중이어도 상담은 자동 종료하지 않는다. |

### Request Header

| 이름 | 값 | 필수 | 설명 |
| --- | --- | --- | --- |
| Authorization | Bearer {accessToken} | Y | 로그인 시 발급받은 JWT |

### Response Body

```json
{
  "success": true,
  "message": "로그아웃되었습니다.",
  "data": null
}
```

### Status Code

| 상태 코드 | 설명 |
| --- | --- |
| 200 OK | 로그아웃 성공 |
| 401 Unauthorized | 토큰 없음·만료 |

### 예외 상황

| 상황 | 응답 코드 | 메시지 |
| --- | --- | --- |
| 토큰 없음·만료 | 401 Unauthorized | 로그인이 필요합니다. |

---

## API-AUTH-05 내 정보 조회

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API ID | API-AUTH-05 |
| 기능 | 내 정보 조회 |
| Method | GET |
| URL | /api/auth/me |
| 사용 화면 | SCR-001 메인, SCR-003 로그인, SCR-004 고객 메인 |
| 호출 시점 | 화면 진입·새로고침 시 |
| 인증 필요 | 예 |
| 관련 요구사항 | NFR-002, NFR-003 |
| 설명 | 토큰으로 현재 사용자를 확인한다. 프론트 라우트 가드가 역할을 확인할 때 쓴다. 제재 상태는 GET /api/customers/me/sanction(타 담당)에서 받는다. |

### Request Header

| 이름 | 값 | 필수 | 설명 |
| --- | --- | --- | --- |
| Authorization | Bearer {accessToken} | Y | 로그인 시 발급받은 JWT |

### Response Body

```json
{
  "success": true,
  "message": "요청이 처리되었습니다.",
  "data": {
    "memberId": 1,
    "loginId": "user01",
    "name": "홍길동",
    "role": "CUSTOMER"
  }
}
```

### Status Code

| 상태 코드 | 설명 |
| --- | --- |
| 200 OK | 조회 성공 |
| 401 Unauthorized | 토큰 없음·만료·로그아웃된 토큰 |

### 예외 상황

| 상황 | 응답 코드 | 메시지 |
| --- | --- | --- |
| 토큰 없음·만료·로그아웃된 토큰 | 401 Unauthorized | 로그인이 필요합니다. |
