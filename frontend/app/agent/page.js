"use client";

import { useCallback, useEffect, useLayoutEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import Icon from "../_components/Icon";
import { AgentHeader } from "../_components/Headers";
import { callApi, formatSeconds } from "./_lib/api";

// 대기열은 폴링으로 갱신한다 (contract.md WebSocket 「대기열」, WBS-37)
const POLL_MS = 3000;

export default function AgentConsolePage() {
  const router = useRouter();
  const [queue, setQueue] = useState(null);
  const [loadError, setLoadError] = useState("");
  const [lastUpdated, setLastUpdated] = useState(null);
  const [recovery, setRecovery] = useState(null);
  const [now, setNow] = useState(() => Date.now());
  const [selectedId, setSelectedId] = useState(null);
  const [acceptingId, setAcceptingId] = useState(null);
  const [acceptError, setAcceptError] = useState("");

  // Cache Components 는 다른 화면으로 가도 이 화면을 숨겨 두고 상태를 그대로 살린다 (Next.js Activity).
  // 수락에 성공하면 바로 채팅 화면으로 가서 acceptingId 가 남는데, 돌아왔을 때 수락 버튼이 모두 잠겨 있지 않도록
  // 화면이 숨겨질 때 버튼 상태를 되돌린다 (#76 StartChatButton 과 같은 방식).
  useLayoutEffect(() => {
    return () => {
      setAcceptingId(null);
      setAcceptError("");
    };
  }, []);

  // 상담 대기열 (API-001) + 회복 시간·진행 중 상담 (API-008)
  const refresh = useCallback(async () => {
    const [queueResult, recoveryResult] = await Promise.all([
      callApi("/api/chats/queue", "GET", "상담 대기열을 불러오지 못했습니다."),
      callApi("/api/agents/me/recovery", "GET", "상담원 상태를 불러오지 못했습니다."),
    ]);
    if (queueResult.unauthorized || recoveryResult.unauthorized) {
      router.replace("/login");
      return;
    }
    if (queueResult.error) {
      setLoadError(queueResult.error);
    } else {
      setQueue(queueResult.data);
      setLoadError("");
      setLastUpdated(new Date());
    }
    if (recoveryResult.data) {
      setRecovery({ ...recoveryResult.data, receivedAt: Date.now() });
    }
  }, [router]);

  useEffect(() => {
    let cancelled = false;
    const tick = () => {
      if (!cancelled) refresh();
    };
    tick();
    const id = setInterval(tick, POLL_MS);
    return () => {
      cancelled = true;
      clearInterval(id);
    };
  }, [refresh]);

  // 회복 시간 카운트다운
  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(id);
  }, []);

  const remainingSeconds = recovery?.isRecovering
    ? Math.max(0, recovery.remainingSeconds - Math.floor((now - recovery.receivedAt) / 1000))
    : 0;
  const recovering = remainingSeconds > 0;
  const activeChatId = recovery?.activeConsultationId ?? null;
  const acceptBlocked = recovering || activeChatId !== null;

  // 상담 수락 (API-002). 실패하면 서버 메시지를 보여주고 목록을 새로고침한다.
  async function handleAccept(chatId) {
    setAcceptingId(chatId);
    setAcceptError("");
    const result = await callApi(`/api/chats/${chatId}/accept`, "PATCH", "상담을 수락하지 못했습니다.");
    if (result.unauthorized) {
      router.replace("/login");
      return;
    }
    if (result.error) {
      setAcceptError(result.error);
      setAcceptingId(null);
      refresh();
      return;
    }
    router.push(`/agent/chat?chatId=${result.data.consultationId}`);
  }

  const selected = queue?.find((q) => q.consultationId === selectedId) ?? queue?.[0] ?? null;

  return (
    <>
      <title>상담 대기열 · SafeDesk 상담원</title>
      <AgentHeader active="queue" busy={activeChatId !== null} />
      <main className="container-wide main">
        <div className="between">
          <h1 style={{ fontSize: 24 }}>상담 대기열</h1>
          <span className="small muted">
            {POLL_MS / 1000}초마다 갱신 · 마지막 갱신{" "}
            {lastUpdated
              ? lastUpdated.toLocaleTimeString("ko-KR", { hour12: false })
              : "-"}
          </span>
        </div>

        {recovering && (
          <div role="status" className="alert alert-warn">
            <Icon name="warn" size={18} />
            <span>
              <b>휴식 시간입니다.</b> 폭언으로 종료된 상담 뒤 {formatSeconds(remainingSeconds)} 동안
              새 상담을 수락할 수 없습니다.
            </span>
          </div>
        )}

        {activeChatId !== null && (
          <div role="status" className="alert">
            <span>
              진행 중인 상담이 있습니다. 먼저 종료해야 새 상담을 수락할 수 있습니다.{" "}
              <Link href={`/agent/chat?chatId=${activeChatId}`}>진행 중 상담으로 돌아가기</Link>
            </span>
          </div>
        )}

        <div
          className="stats"
          style={{
            gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))",
          }}
        >
          <div className="stat">
            <span>대기 중 (QUEUED)</span>
            <b style={{ color: "var(--primary)" }}>
              {queue ? queue.length : "-"}
            </b>
          </div>
          <div className="stat">
            <span>내 진행 중 (ACTIVE)</span>
            <b>{recovery ? (activeChatId !== null ? 1 : 0) : "-"}</b>
          </div>
          <div className="stat">
            <span>수락 가능</span>
            <b style={{ color: acceptBlocked ? "var(--warn)" : "var(--primary)" }}>
              {recovery ? (acceptBlocked ? "불가" : "가능") : "-"}
            </b>
          </div>
        </div>

        <div className="row">
          <section
            className="card card-flush"
            style={{ flex: "999 1 560px", minWidth: 0 }}
          >
            <div className="table-wrap">
              <table style={{ minWidth: 640 }}>
                <thead>
                  <tr>
                    <th>순번</th>
                    <th>고객</th>
                    <th>AI 상담 요약</th>
                    <th>대기 시간</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {queue?.map((q, i) => (
                    <tr
                      key={q.consultationId}
                      onClick={() => setSelectedId(q.consultationId)}
                      style={{
                        cursor: "pointer",
                        background:
                          selected?.consultationId === q.consultationId
                            ? "var(--primary-soft)"
                            : undefined,
                      }}
                    >
                      <td>{i + 1}</td>
                      <td>{q.customerId}</td>
                      <td>{q.inquirySummary ?? q.inquiryCategory ?? "-"}</td>
                      <td>{formatSeconds(q.waitingTime)}</td>
                      <td style={{ textAlign: "right", padding: "10px 16px" }}>
                        <button
                          type="button"
                          className="btn btn-primary btn-sm"
                          disabled={acceptingId !== null || acceptBlocked}
                          onClick={(e) => {
                            e.stopPropagation();
                            handleAccept(q.consultationId);
                          }}
                        >
                          {acceptingId === q.consultationId
                            ? "수락 중..."
                            : "수락"}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {loadError && <div className="card-foot">{loadError}</div>}
            {!loadError && queue?.length === 0 && (
              <div className="card-foot">대기 중인 상담이 없습니다.</div>
            )}
          </section>

          <aside className="agent-side">
            <section className="card stack gap-12" style={{ padding: 20 }}>
              <h2 style={{ fontSize: 16 }}>선택한 상담 미리보기</h2>
              {selected ? (
                <>
                  <span className="small muted">
                    상담번호 C-{String(selected.consultationId).padStart(6, "0")} · 고객{" "}
                    {selected.customerId} · 대기 {formatSeconds(selected.waitingTime)}
                  </span>
                  <div className="kv" style={{ padding: 0, border: 0 }}>
                    <span>문의 분류</span>
                    <span>{selected.inquiryCategory ?? "-"}</span>
                  </div>
                  <div className="kv" style={{ padding: 0, border: 0 }}>
                    <span>AI 상담 요약</span>
                    <span>{selected.inquirySummary ?? "-"}</span>
                  </div>
                  <span className="small muted">
                    AI 상담 전체 내역은 수락 후 채팅 화면에서 볼 수 있습니다.
                  </span>
                </>
              ) : (
                <span className="small muted">대기 중인 상담이 없습니다.</span>
              )}
            </section>
            {acceptError && (
              <div
                role="status"
                className="alert alert-danger"
                style={{ alignItems: "flex-start", flexWrap: "nowrap" }}
              >
                <span style={{ marginTop: 3 }}>
                  <Icon name="alert" size={18} />
                </span>
                <span>
                  <b>수락 실패</b>
                  <br />
                  {acceptError}
                </span>
              </div>
            )}
          </aside>
        </div>
      </main>
    </>
  );
}
