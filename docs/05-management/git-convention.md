# Git 규칙 / 코드 컨벤션

> 출처: Notion 「Git 규칙 / 코드 컨벤션」 (마지막 수정: 2026-11-09)

```txt
1. 브랜치 규칙
2. 커밋 메시지 규칙
3. Pull Request 규칙
4. 파일/폴더명 규칙
5. Java 코드 컨벤션
6. JavaScript/Next.js 코드 컨벤션
7. 보안 파일 관리 규칙
```

# Git 협업 규칙

> **AI 상담원 보호 및 자동 제재 플랫폼**
>
> **5명 팀 · 7일 개발 · 직접 코딩**
>
> 개발 기간: 2026.11.11 ~ 11.19 (11/14~15 제외)

## 1. Git 브랜치 운영 규칙

본 프로젝트는 **`main` → `dev` → 기능별 브랜치** 구조로 협업한다.

- `main`: 최종 배포 및 발표용 안정 버전
- `dev`: 팀원들의 기능을 통합하고 테스트하는 개발 브랜치
- `feat/*`, `fix/*` 등: 팀원별 기능 개발 브랜치

모든 팀원은 `main`과 `dev`에서 직접 기능을 개발하지 않고, **`dev`를 기준으로 기능 브랜치를 생성한 뒤 PR을 통해 `dev`에 병합**한다.

### 브랜치 구조

```txt
main
 └── dev
      ├── feat/customer-chat
      ├── feat/mock-faq
      ├── feat/agent-console
      ├── feat/chat-websocket
      ├── feat/abuse-detection
      ├── feat/sanction-engine
      ├── feat/auth
      ├── feat/admin-sanctions
      ├── feat/chat-history
      └── feat/dashboard
```

### 브랜치 종류

| 브랜치 | 용도 | 병합 대상 |
| --- | --- | --- |
| `main` | 최종 배포·발표용 안정 코드 | 최종 검증된 `dev` |
| `dev` | 개발 기능 통합 및 테스트 | `main` |
| `feat/` | 새로운 기능 개발 | `dev` |
| `fix/` | 개발 중 오류 수정 | `dev` |
| `docs/` | 문서 작성 및 수정 | `dev` |
| `refactor/` | 기능 변경 없는 코드 개선 | `dev` |
| `test/` | 테스트 코드 추가 | `dev` |
| `chore/` | 프로젝트 설정 및 환경 구성 | `dev` |
| `hotfix/` | 배포된 main의 긴급 오류 수정 | `main`, 이후 `dev`에도 반영 |

### 브랜치 운영 원칙

1. `main`과 `dev`에 직접 commit 또는 push하지 않는다.
2. 새로운 기능은 최신 `dev`를 기준으로 브랜치를 생성한다.
3. 기능별 브랜치에서 직접 코딩하고 테스트한다.
4. 개발이 완료되면 PR을 생성하여 `dev`에 병합한다.
5. `dev`에서 통합 테스트를 진행한다.
6. 검증된 `dev`만 PR을 통해 `main`에 병합한다.
7. 병합이 완료된 기능 브랜치는 삭제한다.
8. 다른 팀원의 브랜치에 임의로 push하지 않는다.

### 팀원별 브랜치 예시

| 담당자 | 담당 기능 | 브랜치 예시 |
| --- | --- | --- |
| 심소민 | 고객 채팅·Mock AI 응대 | `feat/customer-chat`, `feat/mock-faq` |
| 정혜영 | 상담원 콘솔·실시간 채팅 | `feat/agent-console`, `feat/chat-websocket` |
| 한수민 | 폭언 감지·마스킹·제재 | `feat/abuse-detection`, `feat/sanction-engine` |
| 장건웅 | 회원·인증·관리자 | `feat/auth`, `feat/admin-sanctions` |
| 유능한 | 상담 이력·대시보드 | `feat/chat-history`, `feat/dashboard` |

---

## 2. Commit 메시지 규칙

### 기본 형식

`type: 작업 내용`

| Type | 사용 상황 | 예시 |
| --- | --- | --- |
| `feat` | 새로운 기능 추가 | `feat: FAQ 기반 Mock 상담 응답 구현` |
| `fix` | 오류 수정 | `fix: 상담 중복 수락 오류 수정` |
| `refactor` | 코드 구조 개선 | `refactor: 폭언 감지 로직 분리` |
| `style` | UI 스타일 수정 | `style: 상담원 채팅 화면 개선` |
| `docs` | 문서 수정 | `docs: API 명세서 업데이트` |
| `test` | 테스트 코드 추가·수정 | `test: 자동 종료 테스트 추가` |
| `chore` | 환경 설정 및 기타 | `chore: MySQL 연결 설정 추가` |

### Commit 작성 규칙

- 커밋 메시지는 한국어로 작성한다.
- 하나의 커밋에는 가능한 한 하나의 논리적 변경만 포함한다.
- 기능과 관계없는 파일은 함께 커밋하지 않는다.
- 커밋 전 `git status`, `git diff`로 변경 사항을 확인한다.
- 코드 실행 및 테스트 후 커밋한다.
- `.env`, API 키, 실제 비밀번호, 개인정보는 커밋하지 않는다.
- `git add .` 사용 전 추가될 파일을 반드시 확인한다.

---

## 3. Pull Request 규칙

모든 기능 개발은 **기능 브랜치 → `dev`** 방향으로 PR을 생성한다.

최종 배포 시에는 **`dev` → `main`** 방향으로 별도의 PR을 생성한다.

### PR 운영 기준

| 항목 | 규칙 |
| --- | --- |
| 기능 개발 PR | `feat/*` → `dev` |
| 오류 수정 PR | `fix/*` → `dev` |
| 최종 배포 PR | `dev` → `main` |
| PR 제목 | Commit 메시지와 동일한 형식 |
| PR 설명 | 변경 사항, 테스트 결과, API·DB 영향 |
| 리뷰어 | 본인 외 팀원 최소 1명 |
| 기능 PR 병합 방식 | Squash and merge 권장 |
| 배포 PR 병합 방식 | 일반 Merge commit 권장 |
| 병합 조건 | 충돌 해결, 빌드 성공, 관련 기능 정상 동작 |
| 병합 후 | 기능 브랜치 삭제, `dev`와 `main`은 유지 |

### PR 설명 템플릿

```txt
## 관련 요구사항
- REQ-000

## 변경 사항
-

## 테스트 결과
- [ ] 프론트엔드 빌드 성공 (해당 시)
- [ ] 백엔드 빌드 성공 (해당 시)
- [ ] 변경한 기능 정상 동작
- [ ] 기존 기능 영향 확인

## API / DB 변경 사항
- API 변경: 없음
- DB 변경: 없음
- WebSocket 이벤트 변경: 없음

## 주의 사항
- 없음

## 리뷰 요청 사항
-
```

### PR 리뷰 규칙

1. 본인이 작성한 PR은 직접 승인하지 않는다.
2. 관련 기능 담당자를 우선 리뷰어로 지정한다.
3. 공통 파일 변경 시 해당 담당자의 확인을 받는다.
4. 충돌이나 빌드 오류가 있으면 병합하지 않는다.
5. API·DB 변경 사항은 관련 팀원에게 공유한다.
6. `dev`에서 발견된 통합 오류는 별도의 `fix/*` 브랜치에서 수정한다.
7. `dev` → `main` PR은 최종 통합 테스트를 완료한 뒤 병합한다.

---

## 4. 파일 수정 및 충돌 방지 규칙

### 담당 기능별 수정 원칙

| 담당자 | 주요 담당 영역 |
| --- | --- |
| 심소민 | 고객 채팅, FAQ Mock 응대, 상담원 이관 |
| 정혜영 | 상담원 대기열, WebSocket, 상담 상태·메시지 |
| 한수민 | 폭언 감지, 마스킹, 경고, 자동 종료, 제재 |
| 장건웅 | 회원가입, 로그인, JWT, 관리자 기능, 공통 UI |
| 유능한 | 상담 이력, 통계, 공통 DB 구조, 배포 |

### 파일 수정 규칙

- 본인 담당 기능의 파일을 중심으로 작업한다.
- 다른 팀원의 파일을 수정해야 한다면 사전에 공유한다.
- 공통 파일은 담당자와 협의 후 수정한다.
- 동일한 파일을 여러 명이 동시에 수정하지 않도록 작업 범위를 조정한다.
- API 요청·응답 형식, DB 구조, WebSocket 이벤트를 임의로 변경하지 않는다.
- 공통 코드 변경 시 PR에 변경 내용과 영향 범위를 작성한다.

### 공통 파일 관리

| 영역 | 담당자 |
| --- | --- |
| 공통 UI·전역 CSS | 장건웅 |
| 인증·권한 관련 코드 | 장건웅 |
| WebSocket 및 상담 상태 | 정혜영 |
| 폭언 감지·제재 정책 | 한수민 |
| DB 스키마·공통 응답 형식 | 유능한 |
| 배포 설정 | 유능한 |

**주의:** `dev` 브랜치를 사용한다고 Git 충돌 자체가 없어지는 것은 아니다. 기능별 브랜치와 파일 담당 구분으로 동시 수정 가능성을 줄이고, `dev`에서 충돌을 해결한 뒤 안정적인 코드만 `main`에 반영한다.

---

## 5. Git 충돌 해결 규칙

### 충돌 해결 절차

1. 충돌이 발생한 파일과 변경 내용을 확인한다.
2. 해당 파일 담당자에게 충돌 내용을 공유한다.
3. 필요한 코드가 누락되지 않도록 충돌을 해결한다.
4. 충돌 해결 후 빌드 및 기능 테스트를 진행한다.
5. 정상 동작을 확인한 뒤 PR을 갱신한다.

### 금지 사항

- `main`과 `dev`에 직접 push하지 않는다.
- 다른 팀원의 브랜치를 임의로 삭제하지 않는다.
- 다른 팀원의 코드를 임의로 덮어쓰지 않는다.
- 공유 브랜치의 커밋 이력을 임의로 변경하지 않는다.
- 다음 명령어는 팀 합의 없이 사용하지 않는다.

```txt
git push --force
git reset --hard
git clean -fd
```

---

## 6. 실제 Git 작업 순서

### STEP 1. 최초 dev 브랜치 생성

최초 1회, 저장소 관리자가 `main`에서 `dev`를 생성한다.

```txt
git switch main
git pull origin main

git switch -c dev
git push -u origin dev
```

이후 팀원들은 원격 `dev`를 가져와 작업한다.

```txt
git fetch origin
git switch dev
git pull origin dev
```

로컬에 `dev`가 없다면 다음 명령어를 사용한다.

```txt
git switch --track origin/dev
```

### STEP 2. 최신 dev 기준으로 기능 브랜치 생성

```txt
git status
git switch dev
git pull origin dev

git switch -c feat/abuse-detection
```

### STEP 3. 기능 개발 및 변경 사항 확인

담당 기능을 직접 코딩하고 테스트한다.

```txt
git status
git diff
```

### STEP 4. 변경 사항 커밋

```txt
git add backend/src/main/java/com/example/project/moderation/
git commit -m "feat: 규칙 기반 폭언 감지 기능 구현"
```

### STEP 5. 원격 브랜치 Push

```txt
git push -u origin feat/abuse-detection
```

### STEP 6. dev로 Pull Request 생성

GitHub에서 PR의 대상 브랜치를 반드시 확인한다.

```txt
base: dev
compare: feat/abuse-detection
```

- 변경 사항 작성
- 테스트 결과 작성
- 리뷰어 지정
- API·DB 영향 작성

### STEP 7. 코드 리뷰 및 dev 병합

1. 팀원 최소 1명이 코드를 리뷰한다.
2. 충돌 및 빌드 오류를 해결한다.
3. PR을 `dev`에 병합한다.
4. 병합 후 기능 브랜치를 삭제한다.

### STEP 8. 다음 기능 개발

```txt
git switch dev
git pull origin dev

git switch -c feat/message-masking
```

### STEP 9. dev의 최신 변경 사항 반영

기능 브랜치 작업 중 다른 팀원의 코드가 `dev`에 병합되었다면 필요한 시점에 최신 변경 사항을 가져온다.

먼저 작업 중인 변경 사항을 커밋하거나 안전하게 보관한다.

```txt
git switch feat/message-masking
git fetch origin
git merge origin/dev
```

충돌이 발생하면 관련 담당자와 함께 해결하고 테스트한다.

### STEP 10. dev에서 main으로 최종 병합

통합 테스트 및 배포 준비가 완료되면 GitHub에서 PR을 생성한다.

```txt
base: main
compare: dev
```

- 전체 서비스 흐름 테스트
- 프론트엔드·백엔드 빌드 확인
- DB 및 API 연동 확인
- 팀원 리뷰
- `main`에 병합 및 배포

---

## 7. 일일 Git 협업 규칙

| 날짜 | Git 협업 목표 |
| --- | --- |
| 11/11 (수) | `main`, `dev` 생성 및 브랜치·파일 담당 규칙 확정 |
| 11/12 (목) | 초기 환경·공통 코드 PR → `dev` |
| 11/13 (금) | 핵심 기능별 PR → `dev`, 1차 통합 확인 |
| 11/16 (월) | 고객 상담·상담원 연결·실시간 채팅 통합 |
| 11/17 (화) | 폭언 감지·자동 종료·이용정지 기능 통합 |
| 11/18 (수) | 신규 기능 동결, `dev` 통합 테스트 및 `main` 병합·배포 |
| 11/19 (목) | `main` 기준 최종 시연, 긴급 오류만 수정 |

### 매일 작업 종료 체크리스트

- [ ] 변경 파일 확인
- [ ] 담당 기능 실행 및 테스트
- [ ] 완료된 작업 Commit
- [ ] 기능 브랜치 Push
- [ ] PR 대상이 `dev`인지 확인
- [ ] PR 생성 또는 진행 상태 공유
- [ ] 공통 파일 변경 사항 공유
- [ ] API·DB 변경 사항 공유
- [ ] `dev` 통합 상태 확인
- [ ] 다음 작업 및 연동 일정 확인

---

## 8. GitHub 저장소 설정

`main`과 `dev` 브랜치 모두 보호 설정을 적용하는 것을 권장한다.

| 설정 항목 | main | dev |
| --- | --- | --- |
| 직접 Push 제한 | 제한 | 제한 |
| PR 필수 | 필수 | 필수 |
| 최소 승인 인원 | 1명 | 1명 |
| Force Push | 금지 | 금지 |
| 브랜치 삭제 | 금지 | 금지 |
| 빌드 검사 | 권장 | 권장 |
| 병합 조건 | 최종 통합 테스트 완료 | 담당 기능 테스트 완료 |

저장소 유형과 GitHub 플랜에 따라 적용 가능한 보호 규칙이 다를 수 있다.

---

## 9. 최종 Git 협업 원칙

1. **`main`은 최종 배포용, `dev`는 개발 통합용으로 분리한다.**
2. 모든 기능 브랜치는 최신 `dev`에서 생성한다.
3. `main`과 `dev`에서는 직접 개발하거나 push하지 않는다.
4. 기능 개발 완료 후 PR을 통해 `dev`에 병합한다.
5. 모든 PR은 본인 외 팀원 최소 1명의 리뷰를 받는다.
6. 다른 팀원의 파일 및 공통 코드는 사전 협의 후 수정한다.
7. API·DB·WebSocket 규격 변경 시 관련 팀원에게 공유한다.
8. Git 충돌 발생 시 담당자와 협의하여 해결한다.
9. `dev`에서 통합 테스트를 완료한 코드만 `main`에 병합한다.
10. **11/17까지 핵심 기능 통합, 11/18에 최종 검증 및 main 병합, 11/19에는 안정적인 시연 버전을 유지한다.**

### 최종 협업 흐름

```txt
[기능 개발]
feat/*, fix/*
      ↓
   Pull Request
      ↓
[개발 통합]
     dev
      ↓
  통합 테스트
      ↓
   Pull Request
      ↓
[최종 배포]
     main
```

**핵심: 팀원들은 기능별 브랜치에서 개발하고, `dev`에서 기능을 통합하며, 검증된 코드만 `main`에 반영한다.**

---

# 코드 컨벤션 (Code Convention)

> **AI 상담원 보호 및 자동 제재 플랫폼**
>
> **5명 팀 · 7일 개발 · 직접 코딩**
>
> 적용 기술: Java, Spring Boot, Spring Data JPA, MySQL, JavaScript, React, Next.js

## 1. 코드 컨벤션 기본 원칙

코드 컨벤션은 팀원들이 일관된 방식으로 코드를 작성하여 가독성과 유지보수성을 높이고, 협업 과정에서 발생하는 혼란을 줄이기 위한 규칙이다.

### 기본 규칙

1. 모든 팀원은 동일한 네이밍 및 코드 작성 규칙을 따른다.
2. 변수, 메서드, 클래스명은 역할을 명확하게 나타내도록 작성한다.
3. 프론트엔드와 백엔드는 기능별로 코드를 분리한다.
4. 공통으로 사용하는 코드와 응답 형식은 팀에서 합의한 구조를 따른다.
5. 중복 코드는 가능한 한 공통 함수나 컴포넌트로 분리한다.
6. 복잡한 로직에는 동작 방식보다 구현 의도와 이유를 설명하는 주석을 작성한다.
7. 다른 팀원이 작성한 코드를 수정할 경우 변경 사항을 공유한다.
8. 코드 스타일만 변경하는 작업과 기능 변경 작업은 가능한 한 분리한다.

---

## 2. 공통 네이밍 규칙

| 구분 | 규칙 | 예시 |
| --- | --- | --- |
| 클래스명 | PascalCase | `ChatService` |
| 인터페이스명 | PascalCase | `ChatRepository` |
| 변수명 | camelCase | `customerId` |
| 메서드·함수명 | camelCase | `sendMessage()` |
| 상수명 | UPPER_SNAKE_CASE | `MAX_WARNING_COUNT` |
| React 컴포넌트 | PascalCase | `CustomerChat` |
| Java 패키지명 | 소문자 | `moderation` |
| DB 테이블명 | snake_case | `consultation_session` |
| DB 컬럼명 | snake_case | `created_at` |
| API 경로 | 소문자·kebab-case | `/api/v1/chat-sessions` |
| Enum 타입 | PascalCase | `ConsultationStatus` |
| Enum 값 | UPPER_SNAKE_CASE | `TERMINATED` |

### 네이밍 작성 원칙

- 의미를 알 수 없는 축약어 사용을 지양한다.
- 동일한 개념에는 동일한 이름을 사용한다.
- 변수명은 명사 중심으로 작성한다.
- 메서드명은 동사로 시작하여 수행하는 동작을 표현한다.
- Boolean 변수 및 메서드는 `is`, `has`, `can` 등의 접두사를 사용한다.
- 하나의 기능을 여러 이름으로 혼용하지 않는다.

**권장 예시**

```txt
int warningCount;
Long customerId;
boolean isSanctioned;

public void sendWarning() { }
public boolean hasActiveSanction() { return false; }
```

**지양할 예시**

```txt
int wc;
Long id2;
boolean flag;

public void process() { }
```

---

## 3. Java / Spring Boot 코드 컨벤션

### 3.1 기본 작성 규칙

| 항목 | 규칙 |
| --- | --- |
| 들여쓰기 | 스페이스 4칸 |
| 클래스명 | PascalCase |
| 메서드·변수명 | camelCase |
| 상수 | UPPER_SNAKE_CASE |
| 패키지명 | 소문자 |
| 중괄호 | 선언문과 같은 줄에서 시작 |
| 접근 제어자 | 필요한 최소 범위로 설정 |
| 의존성 주입 | 생성자 주입 사용 |
| 예외 처리 | 공통 예외 처리 방식 사용 |
| 반환값 | Controller에서 DTO 반환 |
| Entity | API 응답에 직접 노출하지 않음 |

### 3.2 계층별 역할

Spring Boot에서는 Controller, Service, Repository, DTO, Entity의 역할을 구분한다.

| 계층 | 역할 | 예시 |
| --- | --- | --- |
| Controller | HTTP 요청 및 응답 처리 | `ChatController` |
| Service | 비즈니스 로직 처리 | `ChatService` |
| Repository | DB 조회 및 저장 | `ChatRepository` |
| Entity | DB 테이블 매핑 | `ChatMessage` |
| DTO | 계층 간 요청·응답 데이터 전달 | `ChatMessageRequest` |
| Exception | 비즈니스 예외 처리 | `ChatNotFoundException` |

### 3.3 클래스명 규칙

클래스명은 담당 역할을 알 수 있도록 접미사를 통일한다.

| 역할 | 명명 규칙 | 예시 |
| --- | --- | --- |
| Controller | `기능명Controller` | `SanctionController` |
| Service | `기능명Service` | `SanctionService` |
| Repository | `기능명Repository` | `SanctionRepository` |
| Entity | 도메인명 | `Sanction` |
| Request DTO | `기능명Request` | `ChatMessageRequest` |
| Response DTO | `기능명Response` | `ChatMessageResponse` |
| Enum | 도메인명 + Status/Type | `ConsultationStatus` |
| Exception | 예외명 + Exception | `ConsultationNotFoundException` |

### 3.4 의존성 주입 규칙

`@Autowired` 필드 주입보다 생성자 주입을 사용한다.

```txt
@Service
public class ModerationService {

    private final ViolationRepository violationRepository;

    public ModerationService(
            ViolationRepository violationRepository
    ) {
        this.violationRepository = violationRepository;
    }
}
```

Lombok을 사용하는 경우 `@RequiredArgsConstructor`를 사용할 수 있다.

### 3.5 Controller 작성 규칙

- Controller는 요청값 검증과 HTTP 응답 처리를 담당한다.
- 비즈니스 로직은 Service에서 처리한다.
- Entity를 직접 반환하지 않는다.
- API 경로는 복수형 명사를 사용한다.
- 요청 데이터는 DTO로 전달한다.

```txt
@RestController
@RequestMapping("/api/v1/sanctions")
public class SanctionController {

    private final SanctionService sanctionService;

    public SanctionController(SanctionService sanctionService) {
        this.sanctionService = sanctionService;
    }

    @GetMapping("/{customerId}")
    public ApiResponse<SanctionResponse> getSanction(
            @PathVariable Long customerId
    ) {
        SanctionResponse result =
                sanctionService.getSanction(customerId);

        return ApiResponse.success(result);
    }
}
```

`ApiResponse`는 팀에서 별도로 정의한 공통 응답 클래스를 사용한다.

### 3.6 Service 작성 규칙

- 비즈니스 로직은 Service에 작성한다.
- 하나의 메서드는 가능한 한 하나의 명확한 책임을 갖는다.
- DB 변경 작업은 트랜잭션을 고려한다.
- 조회 전용 메서드는 `@Transactional(readOnly = true)`를 사용한다.
- 다른 도메인의 기능을 직접 중복 구현하지 않고 해당 Service를 호출한다.

### 3.7 Repository 작성 규칙

- Spring Data JPA를 사용한다.
- Repository는 DB 접근에 집중한다.
- 복잡한 비즈니스 로직을 Repository에 작성하지 않는다.
- 조회 메서드 이름은 의미를 알 수 있도록 작성한다.

```txt
public interface SanctionRepository
        extends JpaRepository<Sanction, Long> {

    List<Sanction> findByCustomerId(Long customerId);
}
```

---

## 4. JavaScript / React / Next.js 코드 컨벤션

### 4.1 기본 작성 규칙

| 항목 | 규칙 |
| --- | --- |
| 들여쓰기 | 스페이스 2칸 |
| 변수·함수명 | camelCase |
| React 컴포넌트명 | PascalCase |
| 컴포넌트 파일명 | PascalCase |
| 일반 JS 파일명 | camelCase |
| 상수명 | UPPER_SNAKE_CASE |
| 문자열 | 큰따옴표 `"` |
| 세미콜론 | 사용 |
| 변수 선언 | 기본 `const`, 재할당 시 `let` |
| 비동기 처리 | `async/await` 권장 |
| 이벤트 핸들러 | `handle` 접두사 사용 |
| Props | camelCase |

### 4.2 React 컴포넌트 작성 규칙

- 컴포넌트는 하나의 명확한 역할을 갖도록 작성한다.
- 반복되는 UI는 재사용 가능한 컴포넌트로 분리한다.
- API 호출 로직은 가능한 한 별도 파일로 분리한다.
- 상태 변수는 의미를 알 수 있는 이름을 사용한다.
- React Hook은 컴포넌트 최상위에서 호출한다.
- 이벤트 핸들러는 `handle` 접두사를 사용한다.

```txt
"use client";

import { useState } from "react";

export default function CustomerChat() {
  const [message, setMessage] = useState("");
  const [messages, setMessages] = useState([]);

  const handleSendMessage = () => {
    if (!message.trim()) return;

    setMessages((prev) => [...prev, message]);
    setMessage("");
  };

  return (
    <div>
      <input
        value={message}
        onChange={(e) => setMessage(e.target.value)}
      />
      <button onClick={handleSendMessage}>
        전송
      </button>
    </div>
  );
}
```

### 4.3 컴포넌트 파일명 규칙

| 파일 유형 | 예시 |
| --- | --- |
| 고객 채팅 컴포넌트 | `CustomerChat.jsx` |
| 메시지 컴포넌트 | `ChatMessage.jsx` |
| 상담원 대기열 | `AgentQueue.jsx` |
| 폭언 경고 알림 | `WarningAlert.jsx` |
| 제재 고객 목록 | `SanctionList.jsx` |
| 대시보드 차트 | `DashboardChart.jsx` |

Next.js App Router의 예약 파일명인 `page.js`, `layout.js`, `loading.js`, `error.js` 등은 프레임워크 규칙을 따른다.

### 4.4 API 호출 규칙

- API 호출 코드는 화면 컴포넌트와 분리한다.
- 기능별 API 파일을 작성한다.
- API 기본 URL은 환경변수로 관리한다.
- 오류 발생 시 예외를 처리하고 사용자에게 안내한다.
- 동일한 API를 여러 컴포넌트에서 중복 작성하지 않는다.

```txt
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL;

export async function getChatHistory(sessionId) {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/chat-sessions/${sessionId}/messages`
  );

  if (!response.ok) {
    throw new Error("상담 이력을 불러오지 못했습니다.");
  }

  return response.json();
}
```

### 4.5 상태 관리 규칙

- 컴포넌트 내부에서만 사용하는 상태는 `useState`를 사용한다.
- 여러 컴포넌트에서 공유해야 하는 상태는 팀에서 합의한 방식으로 관리한다.
- 서버 데이터와 화면 상태를 구분한다.
- 상담 상태값은 백엔드에서 정의한 값과 동일하게 사용한다.

---

## 5. API 네이밍 및 응답 규칙

### 5.1 API 경로 규칙

- API 경로는 `/api/v1`을 공통 접두사로 사용한다.
- 리소스명은 복수형 명사를 사용한다.
- 경로는 소문자로 작성한다.
- 여러 단어는 하이픈()으로 구분한다.
- 조회·생성·수정·삭제는 HTTP Method로 구분한다.
- 예외적인 상태 변경은 의미가 명확한 하위 경로를 사용한다.

| 기능 | Method | API 예시 |
| --- | --- | --- |
| 상담 시작 | POST | `/api/v1/chat-sessions` |
| 상담 조회 | GET | `/api/v1/chat-sessions/{id}` |
| 상담원 연결 요청 | POST | `/api/v1/chat-sessions/{id}/handoff` |
| 상담 수락 | POST | `/api/v1/chat-sessions/{id}/accept` |
| 상담 종료 | POST | `/api/v1/chat-sessions/{id}/close` |
| 제재 고객 목록 | GET | `/api/v1/sanctions` |
| 제재 해제 | PATCH | `/api/v1/sanctions/{id}/release` |
| 상담 이력 조회 | GET | `/api/v1/chat-history` |
| 통계 조회 | GET | `/api/v1/dashboard/statistics` |

위 경로는 네이밍 예시이며, 실제 구현 경로는 API 명세서에서 최종 확정한다.

### 5.2 API 응답 형식

성공 응답과 실패 응답은 가능한 한 동일한 구조를 사용한다.

**성공 응답**

```txt
{
  "success": true,
  "data": {
    "sessionId": 1,
    "status": "ACTIVE"
  },
  "message": "상담이 연결되었습니다."
}
```

**실패 응답**

```txt
{
  "success": false,
  "data": null,
  "message": "이미 종료된 상담입니다.",
  "errorCode": "SESSION_ALREADY_CLOSED"
}
```

### 5.3 HTTP 상태 코드

| 상태 코드 | 사용 상황 |
| --- | --- |
| `200 OK` | 정상 조회·수정·처리 |
| `201 Created` | 리소스 생성 성공 |
| `400 Bad Request` | 잘못된 입력값 |
| `401 Unauthorized` | 인증되지 않은 사용자 |
| `403 Forbidden` | 접근 권한 없음 |
| `404 Not Found` | 존재하지 않는 데이터 |
| `409 Conflict` | 중복 수락 등 상태 충돌 |
| `500 Internal Server Error` | 서버 내부 오류 |

---

## 6. DB / JPA 컨벤션

### 6.1 DB 네이밍 규칙

| 항목 | 규칙 | 예시 |
| --- | --- | --- |
| 테이블명 | snake_case | `consultation_session` |
| 컬럼명 | snake_case | `customer_id` |
| 기본키 | `id` 또는 합의한 명명 규칙 | `id` |
| 외래키 | 참조 대상 + `_id` | `session_id` |
| 생성 일시 | `created_at` | `created_at` |
| 수정 일시 | `updated_at` | `updated_at` |
| 종료 일시 | `ended_at` | `ended_at` |
| 상태 컬럼 | `_status` 접미사 | `consultation_status` |

### 6.2 JPA 작성 규칙

- Entity 클래스명은 PascalCase를 사용한다.
- Entity 필드명은 camelCase를 사용한다.
- DB 테이블과 컬럼은 snake_case를 사용한다.
- Entity는 API 응답으로 직접 반환하지 않는다.
- 연관관계는 필요한 경우에만 설정한다.
- 양방향 연관관계는 불필요하게 사용하지 않는다.
- Enum은 `EnumType.STRING`으로 저장한다.
- 데이터 변경 작업은 트랜잭션을 고려한다.

```txt
@Entity
@Table(name = "consultation_session")
public class ConsultationSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "consultation_status", nullable = false)
    private ConsultationStatus status;
}
```

### 6.3 Enum 규칙

상담 상태는 모든 팀원이 동일한 값을 사용한다.

```txt
public enum ConsultationStatus {
    BOT,
    QUEUED,
    ACTIVE,
    CLOSED,
    TERMINATED
}
```

| 상태 | 의미 |
| --- | --- |
| `BOT` | Mock AI 상담 중 |
| `QUEUED` | 상담원 연결 대기 |
| `ACTIVE` | 상담원과 상담 중 |
| `CLOSED` | 정상 종료 |
| `TERMINATED` | 정책에 따른 강제 종료 |

폭언 분류는 현재 요구사항에 맞춰 다음 두 단계를 기본으로 한다.

```txt
public enum AbuseLevel {
    NORMAL,
    ABUSIVE
}
```

등급을 추가해야 하는 경우 한수민과 관련 담당자가 협의한 후 변경한다.

---

## 7. 예외 처리 및 로그 규칙

### 7.1 예외 처리

- 예외 상황을 무시하거나 빈 `catch` 블록으로 처리하지 않는다.
- 사용자에게는 이해할 수 있는 오류 메시지를 제공한다.
- 내부 서버 오류나 상세한 예외 정보는 사용자에게 직접 노출하지 않는다.
- 공통 예외 처리 클래스를 사용한다.
- 비즈니스 예외는 의미 있는 예외명 또는 에러 코드를 사용한다.

**예외 상황 예시**

| 상황 | 처리 방식 |
| --- | --- |
| 로그인 실패 | 아이디 또는 비밀번호 오류 안내 |
| 이용정지 고객 상담 요청 | 이용정지 사유·해제 예정일 안내 |
| 이미 수락된 상담 | 중복 수락 거부 |
| 종료된 상담에 메시지 전송 | 전송 요청 거부 |
| AI Mock 응답 실패 | FAQ 대체 응답 또는 상담원 연결 안내 |
| WebSocket 연결 끊김 | 재연결 안내 및 기존 메시지 복원 |
| 권한 없는 원문 조회 | 접근 거부 |

### 7.2 로그 작성 규칙

- 개발 중 확인이 필요한 이벤트는 로그로 기록한다.
- 비밀번호, JWT 토큰, 실제 개인정보를 로그에 출력하지 않는다.
- 폭언 원문은 일반 로그에 남기지 않는다.
- 로그에는 필요한 식별자와 처리 결과만 기록한다.
- 개발용 디버깅 로그는 배포 전에 정리한다.

**백엔드 로그 예시**

```txt
log.info("상담 연결 완료: sessionId={}", sessionId);
log.warn("폭언 감지: sessionId={}, level={}", sessionId, level);
log.error("상담 처리 실패: sessionId={}", sessionId, exception);
```

**프론트엔드 로그**

- 개발 중 `console.log()`를 사용할 수 있다.
- 불필요한 디버깅 로그는 PR 전에 제거한다.
- 사용자 개인정보와 인증 정보를 출력하지 않는다.

---

## 8. 주석 작성 규칙

### 기본 규칙

- 코드만으로 이해할 수 있는 내용에는 불필요한 주석을 작성하지 않는다.
- 복잡한 조건문이나 정책 로직에는 구현 이유를 작성한다.
- 주석은 한국어로 작성한다.
- 수정된 코드와 맞지 않는 오래된 주석은 제거한다.
- TODO 주석은 필요한 경우에만 사용하고 작업 내용을 명확하게 작성한다.

**좋은 예시**

```txt
// 상담원에게 폭언 원문이 노출되지 않도록
// 메시지 전송 전에 반드시 마스킹 처리한다.
String maskedMessage = maskAbusiveWords(message);
```

**지양할 예시**

```txt
// 메시지를 마스킹한다.
String maskedMessage = maskAbusiveWords(message);
```

---

## 9. 코드 포맷팅 및 정적 검사

팀원마다 IDE 설정이 달라도 동일한 형식의 코드를 유지하도록 포맷팅 규칙을 통일한다.

| 구분 | 권장 도구 | 규칙 |
| --- | --- | --- |
| Java | IntelliJ Formatter | 스페이스 4칸 |
| JavaScript / React | Prettier | 스페이스 2칸 |
| JavaScript 정적 검사 | ESLint | 프로젝트 공통 설정 사용 |
| Git | `.gitignore` | 불필요한 파일 제외 |
| 편집기 공통 설정 | `.editorconfig` | 줄 끝·들여쓰기 통일 |

### 공통 포맷팅 규칙

- UTF-8 인코딩 사용
- 파일 끝에 줄바꿈 추가
- 불필요한 공백 제거
- Java는 스페이스 4칸
- JavaScript·JSX는 스페이스 2칸
- 코드 포맷팅은 PR 생성 전에 실행
- 기능 변경과 무관한 대규모 자동 포맷팅은 지양

### .editorconfig 예시

```txt
root = true

[*]
charset = utf-8
end_of_line = lf
insert_final_newline = true
trim_trailing_whitespace = true

[*.{js,jsx,json,css}]
indent_style = space
indent_size = 2

[*.java]
indent_style = space
indent_size = 4
```

---

## 10. 팀원 간 공통 코드 수정 규칙

| 공통 영역 | 담당자 | 변경 시 확인 사항 |
| --- | --- | --- |
| 인증·권한 | 장건웅 | 고객·상담원·관리자 접근 영향 |
| 공통 UI·CSS | 장건웅 | 다른 화면 스타일 영향 |
| WebSocket·상담 상태 | 정혜영 | 고객·상담원 연결 영향 |
| 폭언 감지·마스킹 | 한수민 | 메시지 전달 및 경고 영향 |
| 자동 제재 정책 | 한수민 | 관리자·고객 차단 기능 영향 |
| DB 스키마 | 유능한 | Entity·Repository·API 영향 |
| API 공통 응답 | 유능한 | 전체 프론트엔드 API 처리 영향 |
| 배포 환경 | 유능한 | 실행 환경 및 환경변수 영향 |

### 변경 원칙

1. 공통 코드 변경 전 관련 담당자와 협의한다.
2. 변경 사항은 PR 설명에 작성한다.
3. API 응답 형식이 변경되면 관련 프론트엔드 코드도 확인한다.
4. DB 컬럼이 변경되면 관련 Entity와 DTO를 확인한다.
5. 기존 기능에 영향을 주는지 테스트한다.
6. 다른 팀원의 코드를 임의로 삭제하거나 덮어쓰지 않는다.

---

## 11. 코드 리뷰 체크리스트

PR 생성 및 리뷰 시 다음 사항을 확인한다.

### 공통

- [ ] 네이밍 규칙을 준수했는가?
- [ ] 포맷팅이 일관적인가?
- [ ] 불필요한 코드와 디버깅 로그를 제거했는가?
- [ ] 담당 기능 외의 파일을 불필요하게 수정하지 않았는가?
- [ ] 기존 기능에 영향을 주지 않는가?

### 프론트엔드

- [ ] 컴포넌트 이름과 파일명이 규칙에 맞는가?
- [ ] API 호출 로직이 적절하게 분리되어 있는가?
- [ ] 오류 발생 시 사용자 안내가 표시되는가?
- [ ] 상담 상태에 따른 UI가 정상적으로 변경되는가?

### 백엔드

- [ ] Controller·Service·Repository 역할이 분리되어 있는가?
- [ ] Entity를 API 응답에 직접 노출하지 않는가?
- [ ] 입력값 검증과 예외 처리가 적용되어 있는가?
- [ ] 필요한 트랜잭션 처리가 적용되어 있는가?
- [ ] 권한 검사가 필요한 기능에 적용되어 있는가?

### 프로젝트 핵심 기능

- [ ] 폭언 메시지가 상담원에게 전달되기 전에 마스킹되는가?
- [ ] 상담원에게 폭언 원문이 노출되지 않는가?
- [ ] 경고 및 자동 종료 기준이 정책정의서와 일치하는가?
- [ ] 종료된 상담에 메시지를 전송할 수 없는가?
- [ ] 이용정지된 고객의 상담 시작이 차단되는가?
- [ ] 동일한 위반이 중복 누적되지 않는가?

---

## 12. 최종 코드 컨벤션 요약

| 구분 | 최종 규칙 |
| --- | --- |
| Java 들여쓰기 | 스페이스 4칸 |
| JavaScript 들여쓰기 | 스페이스 2칸 |
| 클래스·컴포넌트 | PascalCase |
| 변수·메서드·함수 | camelCase |
| 상수·Enum 값 | UPPER_SNAKE_CASE |
| DB 테이블·컬럼 | snake_case |
| API 경로 | 소문자·kebab-case |
| Java 의존성 주입 | 생성자 주입 |
| Java 계층 구조 | Controller / Service / Repository |
| API 데이터 전달 | DTO 사용 |
| 예외 처리 | 공통 응답 및 예외 처리 |
| React API 호출 | 별도 API 모듈로 분리 |
| Java 포맷팅 | IntelliJ Formatter |
| JS 포맷팅 | Prettier |
| 주석 | 필요한 경우 한국어로 작성 |
| 공통 코드 변경 | 담당자 사전 협의 |
| 코드 병합 | Git PR 및 리뷰 후 병합 |

---

## 최종 원칙

1. **코드의 가독성과 일관성을 우선한다.**
2. 클래스·변수·메서드 이름은 의미가 명확해야 한다.
3. 프론트엔드와 백엔드의 역할을 분리한다.
4. API·DB·Enum 등 공통 규격을 일관되게 유지한다.
5. 중복 코드를 줄이고 공통 기능을 재사용한다.
6. 공통 코드 변경 시 관련 팀원과 협의한다.
7. 모든 코드는 실행 및 테스트 후 PR을 생성한다.
8. **7일 개발 일정에 맞춰 복잡한 규칙보다 팀원 모두가 지킬 수 있는 실용적인 규칙을 우선한다.**
