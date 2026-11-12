# AI 상담원 보호 및 자동 제재 플랫폼

FAQ 기반 Mock AI가 1차 응대하고, 상담원 연결 후 WebSocket 실시간 채팅을 제공합니다.
상담 중 폭언은 서버에서 감지·마스킹하고 정책에 따라 경고·상담 종료·이용정지를 처리합니다.

## 기술 스택

| 영역 | 기술 |
| --- | --- |
| Backend | Java 21, Spring Boot 4.0.9, Spring Data JPA, Spring Security, JWT, WebSocket |
| Frontend | Next.js 16.4.6, React 19.3.0, JavaScript (App Router) |
| Database | MySQL 8.x |
| Build | Gradle Wrapper, npm 11.19.0 |
| Runtime | Node.js 24 (LTS) |

## 실행 환경

| 구분 | 주소 |
| --- | --- |
| Frontend | http://localhost:3000 |
| Backend | http://localhost:8080 |
| MySQL | localhost:3306 |

## 실행 순서

### 1. MySQL

MySQL 서버 실행 후 `backend/src/main/resources/db/schema.sql`을 실행합니다.
DB(`safedesk`) 생성, 테이블, 시드 데이터(계정·FAQ·금칙어·정책 기본값·샘플 상담)가 한 번에 들어갑니다.

- Workbench: File → Open SQL Script → `schema.sql` → 실행(⚡)
- 터미널(PowerShell): `& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p -e "source backend/src/main/resources/db/schema.sql"`

> 다시 실행하면 테이블을 지우고 다시 만듭니다. 로컬에서 넣은 데이터는 사라집니다.

### 2. Backend

환경변수를 설정합니다. (IntelliJ 실행 구성의 Environment variables 또는 시스템 환경변수)

| 변수 | 설명 |
| --- | --- |
| `DB_USERNAME` | MySQL 계정 |
| `DB_PASSWORD` | MySQL 비밀번호 |
| `JWT_SECRET` | JWT 서명 키 |

```
cd backend
.\gradlew.bat bootRun      # Windows
./gradlew bootRun          # macOS / Linux
```

### 3. Frontend

`frontend/.env.example`을 복사해 `frontend/.env.local`을 만듭니다.

```
cd frontend
npm ci
npm run dev
```

> `npm install`이 아니라 `npm ci`를 사용합니다. (`package-lock.json` 기준으로 동일한 버전 설치)

## 브랜치 규칙

- `main`: 최종 배포·발표용 / `dev`: 개발 통합
- 기능 브랜치는 최신 `dev`에서 생성: `feat/*`, `fix/*`, `docs/*`, `refactor/*`, `test/*`, `chore/*`
- 모든 변경은 PR → `dev`, 리뷰어 1명 승인 후 병합
- 커밋 메시지: `type: 작업 내용` (예: `feat: FAQ 기반 Mock 상담 응답 구현`)

자세한 규칙은 노션 「Git 규칙 / 코드 컨벤션」을 참고합니다.
