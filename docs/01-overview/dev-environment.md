# 개발 환경

> 출처: Notion 「개발 환경」 (마지막 수정: 2026-11-09)

**프로젝트명:** AI 상담원 보호 및 자동 제재 플랫폼

**개발 기간:** 2026.11.11 \~ 2026.11.19 (주말 제외, 총 7일)

**개발 인원:** 5명

**개발 방식:** 직접 코딩 / GitHub 협업

**브랜치 전략:** `main`(최종 배포) / `dev`(개발 통합) / 기능별 브랜치

---

## 1. 개발 환경 개요

개발 환경은 프로젝트를 실행하고 개발하기 위해 사용하는 언어, 프레임워크, 라이브러리, 데이터베이스 및 협업 도구를 정리한 문서이다.

본 프로젝트는 **Next.js 기반 프론트엔드, Spring Boot 기반 백엔드, MySQL 데이터베이스**로 구성한다.

고객의 문의는 FAQ 기반 Mock AI가 1차 응대하며, 상담원 연결 이후 WebSocket을 통해 실시간 채팅을 제공한다. 상담 중 폭언이 발생하면 서버에서 메시지를 감지·마스킹하고, 정책에 따라 경고·상담 종료·이용정지를 처리한다.

팀원별 개발 환경 차이로 인한 오류를 방지하기 위해 주요 기술의 버전을 통일한다.

---

## 2. 공통 개발 환경

| 구분 | 사용 기술 / 도구 | 버전 | 사용 목적 |
| --- | --- | --- | --- |
| 운영체제 | Windows | 10 / 11 | 로컬 개발 환경 |
| IDE | IntelliJ IDEA | 팀원별 확인 | Spring Boot 개발 |
| IDE | Visual Studio Code | 팀원별 확인 | Next.js 개발 |
| JDK | Java | 21 | Spring Boot 실행 |
| 빌드 도구 | Gradle | Wrapper 사용 | 백엔드 빌드 및 의존성 관리 |
| API 테스트 | Postman / IntelliJ HTTP Client | 팀 선택 | REST API 테스트 |
| WebSocket 테스트 | Postman / 브라우저 | - | 실시간 채팅 연결 테스트 |
| 형상 관리 | Git | 2.x | 소스 코드 버전 관리 |
| 원격 저장소 | GitHub | - | 코드 저장 및 PR 관리 |
| 협업 도구 | Notion | - | 기획·설계·회의록 관리 |

---

## 3. Backend

| 구분 | 사용 기술 / 라이브러리 | 버전 | 사용 목적 |
| --- | --- | --- | --- |
| Language | Java | 21 | 백엔드 개발 |
| Framework | Spring Boot | 4.x.x (세부 버전 확정 필요) | 백엔드 애플리케이션 |
| Web | Spring Web MVC | Spring Boot 의존성 기준 | REST API 개발 |
| Security | Spring Security | Spring Boot 의존성 기준 | 인증 및 인가 |
| ORM | Spring Data JPA | Spring Boot 의존성 기준 | DB 연동 |
| Validation | Spring Validation | Spring Boot 의존성 기준 | 요청값 검증 |
| Real-time | Spring WebSocket | Spring Boot 의존성 기준 | 실시간 채팅 |
| Database Driver | MySQL Connector/J | Spring Boot 의존성 기준 | MySQL 연결 |
| JWT | jjwt | 0.12.6 (제안) | JWT 생성 및 검증 |
| Utility | Lombok | Spring Boot 의존성 기준 | 반복 코드 감소 |
| Testing | JUnit / Spring Boot Test | Spring Boot 의존성 기준 | 단위·통합 테스트 |

### 주요 백엔드 기능

| 기능 | 구현 기술 | 담당자 |
| --- | --- | --- |
| 회원가입·로그인·권한 | Spring Security, JWT | 장건웅 |
| FAQ Mock AI 응대 | Spring Service, FAQ 데이터 | 심소민 |
| 상담 시작·이관 | Spring Web, JPA | 심소민 |
| 상담 대기열·수락 | Spring Service, JPA | 정혜영 |
| 실시간 채팅 | Spring WebSocket | 정혜영 |
| 폭언 감지·마스킹 | Java 규칙 기반 로직 | 한수민 |
| 자동 경고·종료·제재 | Spring Service, JPA | 한수민 |
| 관리자 제재 관리 | Spring Web, JPA | 장건웅 |
| 상담 이력·통계 | Spring Data JPA | 유능한 |

### Backend 주요 의존성 예시

`backend/build.gradle`

```txt
dependencies {
    // REST API
    implementation 'org.springframework.boot:spring-boot-starter-webmvc'

    // 인증 및 인가
    implementation 'org.springframework.boot:spring-boot-starter-security'

    // JPA 및 DB
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    runtimeOnly 'com.mysql:mysql-connector-j'

    // 요청값 검증
    implementation 'org.springframework.boot:spring-boot-starter-validation'

    // 실시간 채팅
    implementation 'org.springframework.boot:spring-boot-starter-websocket'

    // JWT
    implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.6'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.6'

    // Lombok
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    // 개발 도구
    developmentOnly 'org.springframework.boot:spring-boot-devtools'

    // 테스트
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}
```

※ Spring Boot 4.x 기준 예시이며, 실제 사용 버전에 따라 의존성 호환성을 확인한다.

---

## 4. Frontend

| 구분 | 사용 기술 / 라이브러리 | 버전 | 사용 목적 |
| --- | --- | --- | --- |
| Language | JavaScript | ES2022 이상 권장 | 프론트엔드 개발 |
| Runtime | Node.js | 22.x LTS 권장 | Next.js 실행 |
| Package Manager | npm | Node.js 설치 버전 기준 | 의존성 관리 |
| Framework | Next.js | 15.x (제안) | 화면 구현 및 라우팅 |
| UI Library | React | 19.x (제안) | 컴포넌트 기반 UI |
| HTTP Client | Fetch API | 브라우저 내장 | REST API 요청 |
| Styling | CSS Modules / Tailwind CSS | 팀 선택 | UI 스타일링 |
| State Management | Context API / Zustand | 팀 선택 | 전역 상태 관리 |
| WebSocket Client | WebSocket API / STOMP Client | 팀 선택 | 실시간 채팅 |
| Formatter | Prettier | 프로젝트 설치 버전 | 코드 스타일 통일 |
| Linter | ESLint | 프로젝트 설치 버전 | 코드 정적 검사 |

### 주요 프론트엔드 화면

| 화면 | 주요 기능 | 담당자 |
| --- | --- | --- |
| 회원가입·로그인 | 계정 생성, 로그인, 권한별 이동 | 장건웅 |
| 고객 상담 화면 | Mock AI 응대, 상담원 연결 | 심소민 |
| 고객 실시간 채팅 | 메시지 송수신, 경고·제재 안내 | 심소민 |
| 상담원 콘솔 | 대기열, 상담 수락, 실시간 채팅 | 정혜영 |
| 폭언·경고 UI | 마스킹 메시지, 경고·종료 표시 | 한수민 |
| 관리자 제재 화면 | 제재 조회·해제, 정책 관리 | 장건웅 |
| 상담 이력 | 상담 목록, 메시지, Mock 요약 | 유능한 |
| 통계 대시보드 | 상담·폭언·이용정지 통계 | 유능한 |

### Frontend 주요 라이브러리 예시

`frontend/package.json`

```txt
{
  "dependencies": {
    "next": "15.x",
    "react": "19.x",
    "react-dom": "19.x"
  }
}
```

※ 실제 설치 시 정확한 호환 버전을 사용하고 `package-lock.json`을 Git에 포함한다.

---

## 5. Database

| 구분 | 사용 기술 | 버전 | 사용 목적 |
| --- | --- | --- | --- |
| DBMS | MySQL | 8.x | 프로젝트 데이터 저장 |
| DB Tool | MySQL Workbench | 8.x | DB 관리 및 SQL 실행 |
| ORM | Spring Data JPA | Spring Boot 의존성 기준 | Entity 기반 DB 접근 |
| Schema | `aicc_platform` (제안) | - | 프로젝트 데이터 관리 |

### 주요 데이터

| 데이터 | 저장 내용 |
| --- | --- |
| 회원 | 고객·상담원·관리자 계정 및 권한 |
| FAQ | Mock AI 질문·답변 |
| 상담 세션 | 상담 상태, 담당 상담원, 시작·종료 시간 |
| 상담 메시지 | 고객·상담원 메시지 |
| 폭언 기록 | 폭언 판정 및 위반 내역 |
| 제재 이력 | 경고 횟수, 위반 횟수, 이용정지 기간 |
| 상담 요약 | 규칙 기반 Mock 상담 요약 |
| 통계 | 상담·폭언·제재 데이터 집계 |

### Schema 생성

```txt
CREATE DATABASE IF NOT EXISTS aicc_platform
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
```

---

## 6. Mock AI 및 폭언 감지

본 프로젝트는 실제 LLM API 대신 FAQ 기반 Mock AI를 사용한다.

| 구분 | 구현 방식 | 사용 목적 |
| --- | --- | --- |
| AI 1차 응대 | FAQ 키워드·규칙 매칭 | 고객 문의 자동 응답 |
| 응답 스트리밍 | 순차 출력 시뮬레이션 | AI 답변 표시 |
| 응답 실패 처리 | 타임아웃·오류 Mock 처리 | 대체 안내 및 상담원 이관 |
| 폭언 감지 | Java 규칙 기반 분류 | 정상·폭언 판정 |
| 메시지 마스킹 | 서버 측 문자열 치환 | 상담원 보호 |
| 자동 경고 | 폭언 횟수 누적 | 고객 경고 |
| 상담 자동 종료 | 정책 조건 검사 | 반복 폭언 시 강제 종료 |
| 자동 이용정지 | 위반 횟수 및 기간 계산 | 상담 이용 제한 |
| 상담 요약 | 규칙 기반 템플릿 | 상담 종료 후 요약 생성 |

### 구현 원칙

- 실제 LLM API를 호출하지 않는다.
- FAQ 및 상담 데이터는 Mock 데이터를 사용한다.
- 폭언 판정은 `NORMAL`, `ABUSIVE`로 구분한다.
- 폭언 메시지는 상담원에게 전달하기 전에 서버에서 마스킹한다.
- 경고·종료·이용정지 기준은 정책정의서를 따른다.
- 폭언 원문은 관리자만 조회할 수 있도록 권한을 제한한다.

---

## 7. Server / 실행 환경

| 구분 | 포트 | 설명 |
| --- | --- | --- |
| Frontend 개발 서버 | 3000 | Next.js 개발 서버 |
| Backend 서버 | 8080 | Spring Boot REST API 및 WebSocket |
| Database 서버 | 3306 | MySQL 서버 |

### 서버 연결 구조

```txt
고객 / 상담원 / 관리자
         |
         v
Next.js (localhost:3000)
         |
    REST API / WebSocket
         |
         v
Spring Boot (localhost:8080)
         |
    Spring Data JPA
         |
         v
MySQL (localhost:3306)
```

---

## 8. 실행 방법

### Backend 실행

**사전 준비**

- JDK 21 설치
- MySQL 서버 실행
- DB Schema 생성
- DB 접속 정보 및 JWT Secret 설정

Windows 기준:

```txt
cd backend
.\gradlew.bat bootRun
```

macOS / Linux 기준:

```txt
cd backend
./gradlew bootRun
```

또는 IntelliJ IDEA에서 Spring Boot Application 클래스를 실행한다.

**Backend 주소:** `http://localhost:8080`

### Frontend 실행

```txt
cd frontend
npm install
npm run dev
```

**Frontend 주소:** `http://localhost:3000`

팀에서 `package-lock.json`을 공유한 이후에는 의존성 재현을 위해 `npm ci` 사용을 권장한다.

### Database 실행

MySQL 서버 실행 후 Schema를 생성한다.

```txt
CREATE DATABASE IF NOT EXISTS aicc_platform
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
```

### 실행 순서

1. MySQL 서버 실행
2. Spring Boot 백엔드 실행
3. Next.js 프론트엔드 실행
4. 브라우저에서 `http://localhost:3000` 접속
5. 테스트 계정으로 기능 확인

---

## 9. 환경 변수 / 설정 파일

### Backend — application.yml

```txt
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/aicc_platform
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

jwt:
  secret: ${JWT_SECRET}
```

※ `ddl-auto: validate`는 기존 DB 테이블 구조를 검증하는 설정이다. 초기 테이블은 별도 SQL 스크립트 또는 마이그레이션으로 생성한다.

### Frontend — .env.local

```txt
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
NEXT_PUBLIC_WS_URL=ws://localhost:8080/ws
```

※ WebSocket 경로는 실제 백엔드 설정에 따라 변경한다.

### 환경 변수 관리 규칙

- DB 비밀번호와 JWT Secret은 GitHub에 업로드하지 않는다.
- `.env.local` 등 개인 환경 설정 파일은 `.gitignore`에 포함한다.
- 환경 변수 이름은 `.env.example` 파일로 공유한다.
- `NEXT_PUBLIC_` 환경 변수에는 공개 가능한 값만 사용한다.
- 운영 환경의 민감 정보는 배포 환경 변수로 관리한다.

---

## 10. Git 및 협업 환경

| 구분 | 사용 도구 | 협업 방식 |
| --- | --- | --- |
| 형상 관리 | Git | 기능별 브랜치 관리 |
| 원격 저장소 | GitHub | Pull Request 및 코드 리뷰 |
| 개발 통합 브랜치 | `dev` | 기능 통합 및 테스트 |
| 최종 배포 브랜치 | `main` | 검증된 코드 관리 |
| 문서 관리 | Notion | 기획·설계·회의록 |
| API 문서 | Notion / Markdown | 요청·응답 형식 공유 |
| DB 문서 | ERD / SQL | 테이블 구조 공유 |

### Git 브랜치 구조

```txt
main
 └── dev
      ├── feat/customer-chat
      ├── feat/chat-websocket
      ├── feat/abuse-detection
      ├── feat/sanction-engine
      ├── feat/auth
      └── feat/dashboard
```

### 협업 규칙

- `main`은 최종 배포용으로 사용한다.
- `dev`는 개발 통합 및 테스트용으로 사용한다.
- 기능별 브랜치는 최신 `dev`에서 생성한다.
- 기능 개발 후 PR을 통해 `dev`에 병합한다.
- `dev`에서 통합 테스트 후 `main`에 병합한다.
- 공통 API·DB·WebSocket 변경 시 관련 팀원에게 공유한다.

---

## 11. 개발 환경 확인 명령어

팀원별 설치 버전을 확인하고 기록한다.

| 확인 항목 | 명령어 |
| --- | --- |
| Java | `java -version` |
| Gradle (Windows) | `.\gradlew.bat --version` |
| Node.js | `node -v` |
| npm | `npm -v` |
| Git | `git --version` |
| MySQL | `mysql --version` |
| Next.js / React | `npm ls next react react-dom` |

Spring Boot 버전은 `build.gradle`의 Plugins 설정에서 확인한다.

---

## 12. 개발 환경 체크리스트

### 공통

- [ ] JDK 21 설치 및 버전 통일
- [ ] Gradle Wrapper 정상 실행
- [ ] Node.js / npm 버전 통일
- [ ] GitHub 저장소 및 `dev` 브랜치 생성
- [ ] IntelliJ / VS Code 설정 완료

### Backend

- [ ] Spring Boot 정확한 버전 확정
- [ ] Spring Security 및 JWT 설정
- [ ] Spring Data JPA 설정
- [ ] Spring WebSocket 설정
- [ ] 공통 API 응답 형식 확정
- [ ] 백엔드 서버 정상 실행

### Frontend

- [ ] Next.js / React 버전 확정
- [ ] API 통신 방식 확정
- [ ] CSS 스타일링 방식 확정
- [ ] WebSocket 클라이언트 방식 확정
- [ ] 프론트엔드 서버 정상 실행

### Database

- [ ] MySQL 버전 확인
- [ ] Schema 생성
- [ ] DB 연결 확인
- [ ] ERD 및 테이블 구조 확정
- [ ] 초기 Mock 데이터 준비

### 연동

- [ ] Frontend → Backend REST API 호출 성공
- [ ] WebSocket 연결 성공
- [ ] 고객·상담원 간 메시지 송수신 확인
- [ ] 폭언 감지·마스킹 연동 확인
- [ ] 자동 종료·제재 상태 반영 확인
- [ ] CORS 및 인증 오류 확인

---

## 13. 작성 시 주의사항

- 팀원 전원이 동일한 JDK 및 Node.js 버전을 사용한다.
- Gradle은 로컬 설치 버전보다 Gradle Wrapper를 사용한다.
- 프론트엔드 의존성 버전은 `package-lock.json`으로 관리한다.
- API 요청·응답 형식은 개발 전에 합의한다.
- WebSocket 통신 방식과 이벤트 형식은 초기에 확정한다.
- 프론트엔드와 백엔드의 포트가 다르므로 CORS 설정을 확인한다.
- WebSocket 연결 및 메시지 처리에도 인증·권한 검사를 적용한다.
- 폭언 메시지는 상담원에게 전달하기 전에 서버에서 마스킹한다.
- 실제 LLM API 대신 FAQ 기반 Mock AI를 사용한다.
- DB 비밀번호, JWT Secret, 실제 개인정보는 GitHub에 업로드하지 않는다.
- 처음 프로젝트를 내려받은 팀원도 실행할 수 있도록 README에 실행 방법을 작성한다.

---

## 14. 최종 개발 환경 요약

| 영역 | 사용 기술 |
| --- | --- |
| Backend | Java 21, Spring Boot 4.x |
| Frontend | Next.js, React, JavaScript |
| Database | MySQL 8.x |
| Authentication | Spring Security, JWT |
| Real-time Chat | Spring WebSocket |
| Mock AI | FAQ 기반 Java 규칙 로직 |
| Moderation | 규칙 기반 폭언 감지·마스킹 |
| ORM | Spring Data JPA |
| Build | Gradle Wrapper, npm |
| Version Control | Git, GitHub |
| Collaboration | Notion, Pull Request |
| Branch Strategy | `main` / `dev` / 기능별 브랜치 |
| Local Ports | 3000 / 8080 / 3306 |

**개발 환경 확정 기준:** 프로젝트 시작일인 2026년 11월 11일에 팀원별 실제 설치 버전을 확인하고, Spring Boot·Next.js·React·Node.js·MySQL의 정확한 버전과 WebSocket 통신 방식을 최종 확정한다.
