# API 명세서

> 출처: Notion 「API 명세서」 (마지막 수정: 2026-11-10)

엔드포인트별 상세(요청·응답 예시)는 도메인별 파일로 나누어 두었다.

| 도메인 | 파일 | 포함 API |
| --- | --- | --- |
| 회원·인증 | [api/auth.md](api/auth.md) | API-AUTH-01 ~ 05 |
| 정책 | [api/policy.md](api/policy.md) | API-ADM-01 ~ 03 |
| 제재 | [api/sanction.md](api/sanction.md) | API-ADM-04 ~ 07 |
| FAQ | [api/faq.md](api/faq.md) | API-ADM-08 ~ 11 |

## API 목록

| API ID | 분류 | Method | URL | 기능명 | 인증 | 상태 | 담당자 | 비고 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| [API-AUTH-01](api/auth.md#api-auth-01-회원가입) | 회원 | POST | `/api/auth/signup` | 회원가입 | 불필요 | 작성완료 | 장건웅 |  |
| [API-AUTH-02](api/auth.md#api-auth-02-아이디-중복-확인) | 회원 | GET | `/api/auth/check-login-id` | 아이디 중복 확인 | 불필요 | 작성완료 | 장건웅 |  |
| [API-AUTH-03](api/auth.md#api-auth-03-로그인) | 인증 | POST | `/api/auth/login` | 로그인 | 불필요 | 작성완료 | 장건웅 |  |
| [API-AUTH-04](api/auth.md#api-auth-04-로그아웃) | 인증 | POST | `/api/auth/logout` | 로그아웃 | 필요 | 작성완료 | 장건웅 |  |
| [API-AUTH-05](api/auth.md#api-auth-05-내-정보-조회) | 인증 | GET | `/api/auth/me` | 내 정보 조회 | 필요 | 작성완료 | 장건웅 |  |
| [API-ADM-01](api/policy.md#api-adm-01-제재-정책-조회) | 정책 | GET | `/api/admin/policies` | 제재 정책 조회 | 필요 (ADMIN) | 작성완료 | 장건웅 |  |
| [API-ADM-02](api/policy.md#api-adm-02-제재-정책-수정) | 정책 | PUT | `/api/admin/policies` | 제재 정책 수정 | 필요 (ADMIN) | 작성완료 | 장건웅 |  |
| [API-ADM-03](api/policy.md#api-adm-03-정책-변경-이력-조회) | 정책 | GET | `/api/admin/policies/histories` | 정책 변경 이력 조회 | 필요 (ADMIN) | 작성완료 | 장건웅 |  |
| [API-ADM-04](api/sanction.md#api-adm-04-제재-목록-조회) | 제재 | GET | `/api/admin/sanctions` | 제재 목록 조회 | 필요 (ADMIN) | 작성완료 | 장건웅 |  |
| [API-ADM-05](api/sanction.md#api-adm-05-제재-상세-조회) | 제재 | GET | `/api/admin/sanctions/{sanctionId}` | 제재 상세 조회 | 필요 (ADMIN) | 작성완료 | 장건웅 |  |
| [API-ADM-06](api/sanction.md#api-adm-06-제재-수동-해제) | 제재 | POST | `/api/admin/sanctions/{sanctionId}/release` | 제재 수동 해제 | 필요 (ADMIN) | 작성완료 | 장건웅 |  |
| [API-ADM-07](api/sanction.md#api-adm-07-위반-원문-조회) | 제재 | GET | `/api/admin/violations/{violationId}/original` | 위반 원문 조회 | 필요 (ADMIN) | 작성완료 | 장건웅 |  |
| [API-ADM-08](api/faq.md#api-adm-08-faq-목록-조회) | FAQ | GET | `/api/admin/faqs` | FAQ 목록 조회 | 필요 (ADMIN) | 작성완료 | 장건웅 |  |
| [API-ADM-09](api/faq.md#api-adm-09-faq-등록) | FAQ | POST | `/api/admin/faqs` | FAQ 등록 | 필요 (ADMIN) | 작성완료 | 장건웅 | could |
| [API-ADM-10](api/faq.md#api-adm-10-faq-수정) | FAQ | PUT | `/api/admin/faqs/{faqId}` | FAQ 수정 | 필요 (ADMIN) | 작성완료 | 장건웅 |  |
| [API-ADM-11](api/faq.md#api-adm-11-faq-삭제) | FAQ | DELETE | `/api/admin/faqs/{faqId}` | FAQ 삭제 | 필요 (ADMIN) | 작성완료 | 장건웅 |  |

## 작성 시 주의사항

- URL은 리소스(명사) 중심으로 작성한다.
- HTTP Method는 조회 GET, 등록 POST, 전체 수정 PUT, 일부 수정 PATCH, 삭제 DELETE를 기준으로 사용한다.
- 인증이 필요한 API는 Authorization 헤더를 반드시 작성한다.
- Request Body와 Response Body는 실제 프론트엔드에서 사용할 JSON 구조와 맞춘다.
- 상태 코드는 성공과 실패 상황을 모두 작성한다.
- API 명세서가 변경되면 화면-API 매핑표와 백엔드 Controller도 함께 수정해야 한다.
