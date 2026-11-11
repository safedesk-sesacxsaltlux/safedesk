# 프로젝트 문서

> 노션 「4팀」 페이지의 하위 문서를 마크다운으로 옮긴 것입니다. (변환일: 2026-11-11)
> 원본은 노션이며, 노션 문서가 수정되면 이 폴더도 함께 갱신해야 합니다.

## 1. 프로젝트 개요

| 문서 | 내용 |
| --- | --- |
| [팀 소개](01-overview/team.md) | 역할·담당자·담당 내용 |
| [프로젝트 개요서](01-overview/project-overview.md) | 기획 배경, 기대 효과, 주요 사용자, 핵심 기능 |
| [유사 서비스 분석](01-overview/similar-services.md) | 유사 서비스 5개 비교와 차별화 방향 |
| [개발 환경](01-overview/dev-environment.md) | 기술 스택·버전, 포트, 실행 방법, 환경 변수 |

## 2. 요구사항 및 범위

| 문서 | 내용 |
| --- | --- |
| [정책정의서](02-requirements/policy.md) | 회원, AI 상담, 상담 상태, 폭언, 제재, 권한, 예외 처리 정책 |
| [요구사항 정의서](02-requirements/requirements.md) | 기능 REQ-001~033, 비기능 NFR-001~014, 모듈별 담당 |

## 3. 화면 및 사용자 흐름

| 문서 | 내용 |
| --- | --- |
| [사용자 시나리오](03-ux/user-scenarios.md) | 시나리오 1~19, 요구사항 연결표 |
| [사용자 흐름도](03-ux/user-flow.md) | 기능별 처리 흐름, 화면 목록 SCR-001~019 |
| [화면 설계서](03-ux/screen-design.md) | 화면별 상세(고객/상담사/관리자), 공통 모달·알림 |

## 4. 데이터 및 API 설계

| 문서 | 내용 |
| --- | --- |
| [ERD / 물리 데이터 모델링](04-design/erd.md) | 논리·물리 모델링 (⚠️ 노션에 이미지로만 있음) |
| [API 명세서](04-design/api-spec.md) | API 45개 목록 · 도메인별 상세: [인증](04-design/api/auth.md), [정책](04-design/api/policy.md), [제재](04-design/api/sanction.md), [FAQ](04-design/api/faq.md), [상담·채팅](04-design/api/chat.md), [고객 상담](04-design/api/customer-chat.md), [폭언 감지](04-design/api/moderation.md), [이력·통계](04-design/api/history.md), [내부 연동](04-design/api/internal.md) |
| [AI Agent 기능 기획서](04-design/ai-agent.md) | 1차 응대 / 폭언 분류 / 상담 요약 에이전트 |
| [목업 데이터](04-design/mock-data.md) | FAQ 시드 데이터 명세 |

## 5. 프로젝트 관리

| 문서 | 내용 |
| --- | --- |
| [WBS / 역할 분담 및 일정표](05-management/wbs.md) | ⚠️ 노션 원문 비어 있음 (실제 WBS는 별도 공유 페이지) |
| [Git 규칙 / 코드 컨벤션](05-management/git-convention.md) | 브랜치·커밋·PR 규칙, Java/JS 코드 컨벤션 |
| [이슈관리](05-management/issues.md) | ⚠️ 현재 템플릿 예시 데이터만 있음 |
| [회의록](05-management/meetings/README.md) | 11/02 ~ 11/11 회의 7건 |
| [Daily Scrum](05-management/daily-scrum/README.md) | 날짜별 팀원 스크럼 (11/11 ~ 11/20, 현재 항목 비어 있음) |

## 6. 시연 준비

| 문서 | 내용 |
| --- | --- |
| [시연 시나리오](06-demo/demo-scenario.md) | ⚠️ 현재 템플릿 예시 내용 |
| [발표 자료 / 발표 영상](06-demo/presentation.md) | ⚠️ 노션 원문 비어 있음 |
