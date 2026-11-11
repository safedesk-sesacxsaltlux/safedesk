# API 명세서 — 내부 이벤트·연동

> 출처: Notion 「API 명세서」 하위 행 INT-01 ~ INT-02 (마지막 수정: 2026-11-11)

[← API 명세서 목차](../api-spec.md)

| API ID | Method | URL | 기능명 | 분류 | 인증 | 상태 | 담당자 | 비고 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| INT-01 | EVENT | `ConsultationClosedEvent` | 상담 요약 생성 | 내부 | 불필요(서버 내부) | 작성완료 | 유능한 |  |
| INT-02 | INTERNAL | `AiGateway.reply()` | AI 게이트웨이 | 내부 | 불필요(서버 내부) | 작성완료 | 심소민 | 타임아웃 10초·mock |

---

## INT-01 상담 요약 생성

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API ID | INT-01 |
| 기능 | 상담 요약 생성 |
| Method | 내부 이벤트 (HTTP 아님) |
| URL | ConsultationClosedEvent → ConsultationSummaryService.summarize() |
| 사용 화면 | 없음 (결과는 HIS-01·03·04·06에서 조회) |
| 호출 시점 | 상담 상태가 CLOSED 또는 TERMINATED로 바뀐 트랜잭션이 커밋된 직후 |
| 인증 필요 | 아니오 (서버 내부 호출) |
| 설명 | 요약 템플릿으로 CONSULTATION의 inquiry_category, inquiry_summary, process_summary, result를 채운다. 종료를 처리하는 쪽(심소민·정혜영·한수민)은 이벤트만 발행한다. (REQ-029) |

### 호출 방법

```java
// 종료 처리 담당이 상태 변경 트랜잭션 안에서 발행
publisher.publishEvent(new ConsultationClosedEvent(consultationId));

// 유능한: 커밋 후에만 실행되므로 요약이 실패해도 종료는 되돌리지 않는다
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onClosed(ConsultationClosedEvent event) {
    consultationSummaryService.summarize(event.consultationId());
}
```

### 저장 규칙

| 저장 컬럼 | 템플릿 규칙 |
| --- | --- |
| inquiry_category | 이미 저장된 값 사용. 없으면 첫 고객 메시지에 FAQ 매칭 규칙을 다시 적용. 그래도 없으면 "기타" |
| inquiry_summary | 매칭된 FAQ의 대표 질문. 매칭 없으면 첫 고객 메시지(마스킹본) 앞 50자 + "…" |
| process_summary | "AI 응대" → (queued_at 있으면) "상담원 연결 요청" → (accepted_at 있으면) "상담원 상담" → 종료 문구. 폭언이 있으면 "(폭언 감지 N회)" 추가 |
| result | TERMINATED → TERMINATED / 대기 취소 → UNRESOLVED / AI 단독 종료: FAQ 매칭 있음 → RESOLVED, 없음 → UNRESOLVED / 상담원 정상 종료 → RESOLVED |

종료 문구

| close_reason | closed_by | 종료 문구 |
| --- | --- | --- |
| NORMAL | CUSTOMER | 고객 종료 |
| NORMAL | AGENT | 상담원 종료 |
| QUEUE_CANCEL | CUSTOMER | 대기 중 취소 |
| ABUSE_AUTO | SYSTEM | 반복 폭언으로 자동 종료 |
| ABUSE_MANUAL | AGENT | 폭언으로 상담원 종료 |

예: "AI 응대 → 상담원 연결 요청 → 상담원 상담 → 반복 폭언으로 자동 종료 (폭언 감지 2회)"

### 예외 상황

| 상황 | 처리 |
| --- | --- |
| 요약 중 예외 발생 | inquiry_summary·process_summary에 "요약 없음" 저장. 상담 상태는 이미 커밋되어 영향 없음 (NFR-006) |
| 같은 이벤트가 두 번 수신 | process_summary가 이미 있으면 아무것도 하지 않음 |
| 상담이 없거나 종료 상태가 아님 | 저장하지 않고 경고 로그만 남김 (NFR-009) |
| 실제 AI로 전환 | summarize() 안에서 AI 게이트웨이를 호출하도록 교체. 이벤트 구조는 유지 (NFR-014) |

---

## INT-02 AI 게이트웨이

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API ID | INT-02 |
| 기능 | AI 게이트웨이 |
| Method | 내부 호출 (HTTP 아님) |
| URL | AiGateway.reply() / AiGateway.summarize() |
| 사용 화면 | 없음 (CUS-02의 답변, INT-01의 요약으로 나타남) |
| 호출 시점 | CUS-02에서 고객 메시지마다 / 실제 AI 전환 후 INT-01 summarize() 안에서 |
| 인증 필요 | 아니오 (서버 내부 호출) |
| 설명 | 모든 AI 호출을 한 곳에서 처리한다. 타임아웃 10초, 목업 모드, 호출 기록을 여기서 맡고, 실패는 예외 대신 결과값으로 돌려준다. 실패 시 대응은 호출하는 기능이 정한다. (NFR-006, 010, 014) |

### 호출 방법

```java
public interface AiGateway {
    AiResult reply(AiReplyRequest request);       // 1차 응대 (CUS-02)
    AiResult summarize(AiSummaryRequest request); // 상담 요약 (INT-01, 실제 AI 전환 후)
}

public record AiResult(
        boolean success,
        String content,
        Long faqId,
        long latencyMs,
        AiFailureType failureType  // TIMEOUT, ERROR / 성공 시 null
) {}

// 호출하는 쪽: 실패도 값으로 받으므로 try-catch 없이 분기한다
AiResult result = aiGateway.reply(request);
if (!result.success()) {
    return faqMatcher.match(content);  // FAQ 대체
}
```

```yaml
ai:
  timeout-ms: 10000
  mock:
    mode: NORMAL   # NORMAL | DELAY | ERROR
```

### 저장 규칙

| 저장 위치 | 내용 |
| --- | --- |
| ai_call_log | chat_id, call_type(REPLY·SUMMARY), success, latency_ms, failure_type, fallback_used, created_at |

### 예외 상황

| 상황 | 처리 |
| --- | --- |
| 10초 초과 | 호출을 끊고 success=false, failureType=TIMEOUT 반환 |
| AI 오류 | success=false, failureType=ERROR 반환. 예외를 밖으로 던지지 않음 |
| 호출 기록 저장 실패 | 경고 로그만 남기고 응답은 그대로 반환. 기록 때문에 상담이 멈추면 안 된다 (NFR-006) |
| 실제 AI로 전환 | MockAiGateway를 실제 구현체로 교체. CUS-02·INT-01 코드는 그대로 (NFR-014) |

### 설계 이유

- 실패를 예외로 던지지 않는다. 응대는 FAQ로 대체하고 요약은 "요약 없음"을 저장하는 식으로 대응이 기능마다 다르다. 예외로 던지면 호출하는 곳마다 try-catch가 필요하고, 하나만 빠져도 상담 처리까지 같이 실패한다.
- 타임아웃과 호출 기록을 게이트웨이 안에서 처리한다. 호출하는 쪽에 맡기면 기능마다 10초 규칙이 달라지고 기록이 빠진다.
- 폭언 분류(classify)는 넣지 않는다. 정책 4장에서 AI 2차 판단을 "구현 불가"로 정했다. 2차에 메서드만 추가한다.
