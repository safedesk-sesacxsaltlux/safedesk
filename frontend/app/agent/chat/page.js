"use client";

import { Suspense, useCallback, useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import Icon from "../../_components/Icon";
import { AgentHeader } from "../../_components/Headers";
import { callApi, formatSeconds, formatTime } from "../_lib/api";
import { openChatSocket } from "../_lib/chatSocket";

const QUICK_REPLIES = [
  "안녕하세요, 상담원입니다. 무엇을 도와드릴까요?",
  "확인 후 바로 안내드리겠습니다. 잠시만 기다려 주세요.",
  "더 궁금하신 점이 있으신가요?",
];

const MAX_LENGTH = 500; // contract.md WebSocket 「메시지 길이」

// useSearchParams()는 Suspense 안에서 써야 빌드(정적 생성)가 된다
export default function AgentChatPage() {
  return (
    <Suspense fallback={null}>
      <AgentChat />
    </Suspense>
  );
}

// 메시지는 messageId로 합쳐서 중복 없이 시간순으로 둔다 (재연결 NFR-008, 체크리스트 E6)
function mergeMessages(prev, incoming) {
  const map = new Map(prev.map((m) => [m.messageId, m]));
  for (const m of incoming) map.set(m.messageId, { ...map.get(m.messageId), ...m });
  return [...map.values()].sort((a, b) =>
    a.createdAt === b.createdAt ? a.messageId - b.messageId : a.createdAt < b.createdAt ? -1 : 1,
  );
}

// 같은 화면에서 ?chatId 만 바뀌면 React 는 컴포넌트를 다시 쓰므로, 이전 상담의 메시지·경고·상태가 남는다.
// Cache Components(Activity)로 화면이 살아 있을 때도 마찬가지라 chatId 를 key 로 줘서 상담마다 새로 그린다
// (#76 ChatRoom 과 같은 방식).
function AgentChat() {
  const chatId = useSearchParams().get("chatId");
  return <AgentChatRoom key={chatId ?? "none"} chatId={chatId} />;
}

function AgentChatRoom({ chatId }) {
  const router = useRouter();
  const validChatId = chatId && /^\d+$/.test(chatId) ? chatId : null;

  const [detail, setDetail] = useState(null);
  const [aiMessages, setAiMessages] = useState([]);
  const [messages, setMessages] = useState([]);
  const [notices, setNotices] = useState([]); // 경고·종료 안내 (시간순으로 메시지 사이에 표시)
  const [status, setStatus] = useState(null);
  const [connected, setConnected] = useState(false);
  const [loadError, setLoadError] = useState("");
  const [sendError, setSendError] = useState("");
  const [input, setInput] = useState("");
  const [closing, setClosing] = useState(false);
  const [closeError, setCloseError] = useState("");
  const [now, setNow] = useState(() => Date.now());

  const socketRef = useRef(null);
  const lastMessageIdRef = useRef(0); // 안내(경고·종료)를 어느 메시지 뒤에 둘지 정할 때 쓴다
  const bodyRef = useRef(null);

  const goLogin = useCallback(() => router.replace("/login"), [router]);

  // 상세(API-003)와 메시지 이력(API-004)을 불러온다. 처음 들어올 때와 다시 연결될 때마다 부른다.
  const loadState = useCallback(async () => {
    if (!validChatId) return;
    const [detailResult, historyResult] = await Promise.all([
      callApi(`/api/chats/${validChatId}`, "GET", "상담 정보를 불러오지 못했습니다."),
      callApi(`/api/chats/${validChatId}/messages`, "GET", "메시지를 불러오지 못했습니다."),
    ]);
    if (detailResult.unauthorized || historyResult.unauthorized) {
      goLogin();
      return;
    }
    if (detailResult.error) {
      setLoadError(detailResult.error);
      return;
    }
    setDetail(detailResult.data);
    setStatus(detailResult.data.consultationStatus);
    if (historyResult.data) setMessages((prev) => mergeMessages(prev, historyResult.data));
    setLoadError("");
  }, [validChatId, goLogin]);

  // 상단 메뉴 「진행 중 상담」처럼 상담번호 없이 들어오면, 진행 중인 상담(API-008)으로 보낸다
  useEffect(() => {
    if (validChatId) return;
    let cancelled = false;
    callApi("/api/agents/me/recovery").then((result) => {
      if (cancelled) return;
      if (result.unauthorized) goLogin();
      else if (result.data?.activeConsultationId) router.replace(`/agent/chat?chatId=${result.data.activeConsultationId}`);
      else setLoadError("진행 중인 상담이 없습니다.");
    });
    return () => {
      cancelled = true;
    };
  }, [validChatId, router, goLogin]);

  // 처음 한 번: 상세·이력·AI 응대 내역(API-007)
  useEffect(() => {
    if (!validChatId) return;
    let cancelled = false;
    (async () => {
      await loadState();
      const ai = await callApi(`/api/chats/${validChatId}/ai-messages`, "GET");
      if (!cancelled && ai.data) setAiMessages(ai.data);
    })();
    return () => {
      cancelled = true;
    };
  }, [validChatId, loadState]);

  // 실시간 연결 (API-005). 연결될 때마다 이력을 다시 불러와 끊긴 동안의 메시지를 채운다.
  useEffect(() => {
    if (!validChatId) return;
    const socket = openChatSocket(validChatId, {
      onConnected: () => {
        setConnected(true);
        loadState();
      },
      onDisconnected: () => setConnected(false),
      onEvent: (event) => {
        switch (event.type) {
          case "MESSAGE":
            // 경고는 이 메시지 바로 뒤에 오므로, 렌더링을 기다리지 않고 바로 기록해 둔다
            lastMessageIdRef.current = Math.max(lastMessageIdRef.current, event.messageId);
            setMessages((prev) => mergeMessages(prev, [event]));
            break;
          case "WARNING_ISSUED":
            setNotices((prev) =>
              prev.some((n) => n.key === `w-${event.warningId}`)
                ? prev
                : [...prev, { key: `w-${event.warningId}`, kind: "warning", count: event.warningCount, afterId: lastMessageIdRef.current }],
            );
            setDetail((d) => (d ? { ...d, warningCount: Math.max(d.warningCount, event.warningCount) } : d));
            break;
          case "CONSULTATION_TERMINATED":
            setStatus("TERMINATED");
            setNotices((prev) =>
              prev.some((n) => n.key === "terminated")
                ? prev
                : [...prev, { key: "terminated", kind: "terminated", afterId: lastMessageIdRef.current }],
            );
            break;
          case "CONSULTATION_CLOSED":
            setStatus("CLOSED");
            setNotices((prev) =>
              prev.some((n) => n.key === "closed")
                ? prev
                : [...prev, { key: "closed", kind: "closed", by: event.closedBy, at: event.closedAt, afterId: lastMessageIdRef.current }],
            );
            break;
          default:
            break;
        }
      },
      onError: (error) => {
        if (error.errorCode === "UNAUTHORIZED") {
          goLogin();
          return;
        }
        if (error.errorCode === "CHAT_CLOSED") loadState();
        setSendError(error.message ?? "메시지를 보내지 못했습니다.");
      },
    });
    socketRef.current = socket;
    return () => {
      socket.close();
      socketRef.current = null;
    };
  }, [validChatId, loadState, goLogin]);

  // 경과 시간
  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(id);
  }, []);

  useEffect(() => {
    const max = messages.reduce((acc, m) => Math.max(acc, m.messageId), 0);
    lastMessageIdRef.current = Math.max(lastMessageIdRef.current, max);
  }, [messages]);

  // 새 메시지가 오면 아래로 스크롤
  useEffect(() => {
    const el = bodyRef.current;
    if (el) el.scrollTop = el.scrollHeight;
  }, [messages, notices]);

  const active = status === "IN_PROGRESS";
  const ended = status === "CLOSED" || status === "TERMINATED";

  function handleSend(text = input) {
    const content = text.trim();
    if (!content || !active) return;
    if (content.length > MAX_LENGTH) {
      setSendError(`메시지는 ${MAX_LENGTH}자까지 보낼 수 있습니다.`);
      return;
    }
    if (!socketRef.current?.send(content)) {
      setSendError("연결이 끊겼습니다. 다시 연결되면 보내주세요.");
      return;
    }
    setSendError("");
    setInput("");
  }

  // 상담 종료 (API-006)
  async function handleClose() {
    if (closing || !validChatId) return;
    setClosing(true);
    setCloseError("");
    const result = await callApi(`/api/chats/${validChatId}/close`, "PATCH", "상담을 종료하지 못했습니다.");
    setClosing(false);
    if (result.unauthorized) {
      goLogin();
      return;
    }
    if (result.error) {
      setCloseError(result.error);
      return;
    }
    setStatus(result.data.consultationStatus);
  }

  const acceptedAt = detail?.acceptedAt ? new Date(detail.acceptedAt).getTime() : null;
  const closedAt = detail?.closedAt ? new Date(detail.closedAt).getTime() : null;
  const elapsed = acceptedAt ? Math.floor(((ended && closedAt ? closedAt : now) - acceptedAt) / 1000) : 0;
  const threshold = detail?.closeThreshold ?? null;
  const warningCount = detail?.warningCount ?? 0;

  // 상담원 연결 뒤 메시지만 채팅 창에 둔다. 연결 전 AI 대화는 오른쪽 「이전 AI 상담 내역」에 있다.
  // 경고·종료 안내는 받은 순간의 마지막 메시지 바로 뒤에 놓는다.
  const aiIds = new Set(aiMessages.map((m) => m.messageId));
  const chatMessages = messages.filter((m) => m.senderType !== "AI" && !aiIds.has(m.messageId));
  const timeline = [];
  for (const n of notices.filter((n) => !chatMessages.some((m) => m.messageId <= n.afterId))) {
    timeline.push(n);
  }
  chatMessages.forEach((m, i) => {
    timeline.push({ kind: "message", key: `m-${m.messageId}`, m });
    const nextId = chatMessages[i + 1]?.messageId ?? Infinity;
    for (const n of notices) {
      if (n.afterId >= m.messageId && n.afterId < nextId) timeline.push(n);
    }
  });

  return (
    <>
      <title>진행 중 상담 · SafeDesk 상담원</title>
      <AgentHeader active="chat" busy={active} />
      <main className="container-wide main">
        <div className="row" style={{ alignItems: "stretch" }}>
          <section className="card agent-chat">
            <div className="chat-top">
              <div className="stack">
                <span className="bold" style={{ fontSize: 16 }}>
                  {detail ? `${detail.customerName ?? `고객 ${detail.customerId}`} 님과 상담 중` : "상담 불러오는 중"}
                </span>
                <span className="muted" style={{ fontSize: 12 }}>
                  상담번호 C-{String(validChatId ?? "").padStart(6, "0")} · {status ?? "-"} · 경과{" "}
                  {formatSeconds(elapsed)} · {connected ? "연결됨" : "연결 중..."}
                </span>
              </div>
              {active ? (
                <button type="button" className="btn btn-sm" disabled={closing} onClick={handleClose}>
                  {closing ? "종료 중..." : "상담 종료"}
                </button>
              ) : (
                <Link href="/agent" className="btn btn-sm">
                  대기열로 돌아가기
                </Link>
              )}
            </div>
            {closeError && (
              <div role="alert" className="alert alert-danger" style={{ margin: "8px 16px" }}>
                {closeError}
              </div>
            )}
            {loadError && (
              <div role="alert" className="alert alert-danger" style={{ margin: "8px 16px" }}>
                {loadError} <Link href="/agent">대기열로 돌아가기</Link>
              </div>
            )}

            <div className="chat-body" ref={bodyRef}>
              {detail?.acceptedAt && (
                <div className="sys-note">상담을 수락했습니다 · {formatTime(detail.acceptedAt)}</div>
              )}

              {timeline.map((item) => {
                if (item.kind === "message") return <ChatMessage key={item.key} m={item.m} />;
                if (item.kind === "warning")
                  return (
                    <div key={item.key} className="alert alert-warn" style={{ padding: "10px 14px" }}>
                      <Icon name="warn" size={18} />
                      <span>
                        고객에게 <b>경고 {item.count}회</b>가 자동 발송되었습니다.
                        {threshold && item.count < threshold
                          ? ` ${threshold - item.count}회 더 감지되면 상담이 자동 종료됩니다.`
                          : ""}
                      </span>
                    </div>
                  );
                if (item.kind === "terminated")
                  return (
                    <div key={item.key} className="alert alert-danger" style={{ padding: "10px 14px" }}>
                      <Icon name="alert" size={18} />
                      <span>고객의 반복 폭언으로 상담이 자동 종료되었습니다 (TERMINATED).</span>
                    </div>
                  );
                return (
                  <div key={item.key} className="sys-note">
                    {item.by === "CUSTOMER" ? "고객이" : "상담원이"} 상담을 종료했습니다 · {formatTime(item.at)}
                  </div>
                );
              })}

              {ended && !notices.some((n) => n.kind !== "warning") && (
                <div className="sys-note">
                  {status === "TERMINATED" ? "폭언으로 종료된 상담입니다." : "종료된 상담입니다."}
                </div>
              )}
            </div>

            <div className="chat-foot">
              <div className="chip-row">
                {QUICK_REPLIES.map((text) => (
                  <button
                    key={text}
                    type="button"
                    className="chip"
                    disabled={!active}
                    onClick={() => setInput(text)}
                  >
                    {text.length > 16 ? `${text.slice(0, 16)}…` : text}
                  </button>
                ))}
              </div>
              {sendError && (
                <div role="alert" className="small" style={{ color: "var(--danger, #c0392b)" }}>
                  {sendError}
                </div>
              )}
              <form
                className="chat-input"
                onSubmit={(e) => {
                  e.preventDefault();
                  handleSend();
                }}
              >
                <label htmlFor="ag-input" className="sr-only">
                  답변 입력
                </label>
                <input
                  id="ag-input"
                  className="input"
                  type="text"
                  value={input}
                  maxLength={MAX_LENGTH}
                  onChange={(e) => setInput(e.target.value)}
                  disabled={!active}
                  placeholder={active ? "답변을 입력하세요" : "종료된 상담입니다"}
                  style={{ borderRadius: 10 }}
                />
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={!active || !connected || !input.trim()}
                  style={{ fontWeight: 500, whiteSpace: "nowrap" }}
                >
                  전송
                </button>
              </form>
            </div>
          </section>

          <aside className="agent-side">
            <section className="card stack gap-12" style={{ padding: 20 }}>
              <h2 style={{ fontSize: 16 }}>고객 정보</h2>
              <div className="kv" style={{ padding: 0, border: 0 }}>
                <span>고객</span>
                <span>
                  {detail ? `${detail.customerName ?? "-"} (ID ${detail.customerId})` : "-"}
                </span>
              </div>
              <div className="kv" style={{ padding: 0, border: 0 }}>
                <span>누적 위반</span>
                <span>{detail ? `${detail.customerViolationCount}회` : "-"}</span>
              </div>
              <div className="stack gap-8">
                <div className="kv" style={{ padding: 0, border: 0 }}>
                  <span>이번 상담 경고</span>
                  <span className="bold" style={{ color: warningCount > 0 ? "var(--warn)" : undefined }}>
                    {warningCount} / {threshold ?? "-"}
                  </span>
                </div>
                {threshold && (
                  <div className="progress">
                    <div style={{ width: `${Math.min(100, (warningCount / threshold) * 100)}%` }} />
                  </div>
                )}
              </div>
            </section>

            <section className="card stack gap-12" style={{ padding: 20 }}>
              <h2 style={{ fontSize: 16 }}>이전 AI 상담 내역</h2>
              {aiMessages.length === 0 ? (
                <span className="small muted">AI 상담 내역이 없습니다.</span>
              ) : (
                <div className="transcript">
                  {aiMessages.map((m) => (
                    <span key={m.messageId}>
                      <b>{m.senderType === "AI" ? "AI" : "고객"}</b> {m.content}
                    </span>
                  ))}
                </div>
              )}
            </section>
          </aside>
        </div>
      </main>
    </>
  );
}

function ChatMessage({ m }) {
  if (m.senderType === "AGENT") {
    return (
      <div className="stack gap-4" style={{ maxWidth: "72%", alignSelf: "flex-end", alignItems: "flex-end" }}>
        <div className="bubble-me bubble-agent" style={{ maxWidth: "100%" }}>{m.content}</div>
        <span className="muted" style={{ fontSize: 12 }}>
          {formatTime(m.createdAt)}
        </span>
      </div>
    );
  }
  if (m.senderType === "CUSTOMER" && m.abuseStatus === "ABUSIVE") {
    // 상담원에게는 마스킹본만 온다. 원문은 관리자만 볼 수 있다 (NFR-005·007)
    return (
      <div className="stack gap-4" style={{ maxWidth: "72%" }}>
        <div className="bubble-masked">
          <Icon name="shield" size={18} />
          <span>{m.content}</span>
        </div>
        <span className="muted" style={{ fontSize: 12 }}>
          {formatTime(m.createdAt)} · 부적절한 표현이 가려졌습니다 · 원문은 관리자만 열람 가능
        </span>
      </div>
    );
  }
  if (m.senderType === "CUSTOMER") {
    return (
      <div className="stack gap-4" style={{ maxWidth: "72%" }}>
        <div className="bubble">{m.content}</div>
        <span className="muted" style={{ fontSize: 12 }}>
          {formatTime(m.createdAt)}
        </span>
      </div>
    );
  }
  // AI·SYSTEM 메시지 (상담원 연결 전 대화는 오른쪽 「이전 AI 상담 내역」에도 있다)
  return (
    <div className="sys-note">
      {m.senderType === "AI" ? "AI" : "안내"} · {m.content}
    </div>
  );
}
