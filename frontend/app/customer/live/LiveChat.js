"use client";

import { useCallback, useEffect, useLayoutEffect, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Icon from "../../_components/Icon";
import { chatPath, closeChat, getChat, getMessages } from "../_lib/chatApi";
import { openChatSocket } from "../../agent/_lib/chatSocket";

const MAX_LENGTH = 500;

// SCR-007 고객 실시간 채팅 (API-003·004·005·006, WebSocket)
// Cache Components 는 이 화면을 숨겨 두었다가 다시 쓴다. 상담 번호가 바뀌면 이전 상담 상태가
// 남지 않도록 chatId 를 key 로 줘서 새로 그린다.
export default function LiveChat() {
  const chatId = useSearchParams().get("chatId");
  return <LiveChatContent key={chatId ?? "none"} chatId={chatId} />;
}

function AgentAvatar() {
  return (
    <div className="avatar" style={{ background: "var(--line-soft)", color: "var(--text)" }}>
      <Icon name="user" size={18} />
    </div>
  );
}

function LiveChatContent({ chatId }) {
  const router = useRouter();

  const [agentName, setAgentName] = useState("");
  const [messages, setMessages] = useState([]);
  // { count, message } — 경고가 없으면 null
  const [warning, setWarning] = useState(null);
  // 자동 종료 기준 (이 상담에 적용된 정책). 경고 이벤트에는 없어서 상세 조회 값을 따로 둔다
  const [threshold, setThreshold] = useState(null);
  const [input, setInput] = useState("");
  // null: 처음 연결 중, true: 연결됨, false: 끊김 (끊겼을 때만 안내 띠를 보여준다)
  const [connected, setConnected] = useState(null);
  const [closing, setClosing] = useState(false);
  const [error, setError] = useState(null);
  const socketRef = useRef(null);
  const sentRef = useRef([]);
  const endRef = useRef(null);

  // 화면이 숨겨질 때 「종료하는 중」 잠금을 푼다 (Cache Components)
  useLayoutEffect(() => {
    return () => setClosing(false);
  }, []);

  useEffect(() => {
    endRef.current?.scrollIntoView({ block: "end" });
  }, [messages, warning]);

  useEffect(() => {
    if (!chatId) {
      router.replace("/customer");
      return;
    }

    // 상담 상태·경고 수·메시지를 불러온다. 처음 들어올 때와 다시 연결될 때 (contract.md 재연결)
    async function load() {
      const detail = await getChat(chatId);
      if (detail.status === 401) {
        router.replace("/login");
        return;
      }
      if (!detail.body?.success) {
        router.replace("/customer");
        return;
      }
      const chat = detail.body.data;
      // 상담 중이 아니면 상태에 맞는 화면으로 (대기 중, 이미 종료 등)
      if (chat.consultationStatus !== "IN_PROGRESS") {
        router.replace(chatPath(chat.consultationStatus, chatId));
        return;
      }
      setAgentName(chat.agentName ?? "");
      setThreshold(chat.closeThreshold ?? null);
      if (chat.warningCount > 0) {
        setWarning((prev) => ({ ...prev, count: chat.warningCount }));
      } else {
        setWarning(null);
      }

      const history = await getMessages(chatId);
      if (history.body?.success) {
        // AI 상담 때 대화는 빼고, 상담원이 수락한 뒤의 대화만 보여준다
        const acceptedAt = chat.acceptedAt ?? "";
        setMessages(history.body.data.filter((m) => m.senderType !== "AI" && m.createdAt >= acceptedAt));
      }
    }

    function handleEvent(event) {
      switch (event.type) {
        case "MESSAGE": {
          const original = event.senderType === "CUSTOMER" ? sentRef.current.shift() : undefined;
          const message = original ? { ...event, original } : event;
          // 다시 연결할 때 load 와 겹칠 수 있어서 같은 메시지는 한 번만 넣는다
          setMessages((prev) => (prev.some((m) => m.messageId === event.messageId) ? prev : [...prev, message]));
          break;
        }
        case "WARNING_ISSUED":
          setWarning({ count: event.warningCount, message: event.message });
          break;
        case "CONSULTATION_CLOSED":
        case "CONSULTATION_TERMINATED":
          router.replace(chatPath(event.status, chatId));
          break;
        default:
          break;
      }
    }

    const socket = openChatSocket(chatId, {
      onEvent: handleEvent,
      onError: (e) => {
        sentRef.current.shift();
        setError(e.message ?? "메시지를 보내지 못했습니다.");
      },
      onConnected: () => {
        setConnected(true);
        setError(null);
        load().catch(() => setError("상담 정보를 불러오지 못했습니다."));
      },
      onDisconnected: () => setConnected(false),
    });
    socketRef.current = socket;

    // 화면을 떠나거나 숨겨지면 연결을 끊는다
    return () => {
      socket.close();
      socketRef.current = null;
    };
  }, [chatId, router]);

  const send = useCallback(
    (e) => {
      e.preventDefault();
      const content = input.trim();
      if (!content) return;
      if (content.length > MAX_LENGTH) {
        setError(`메시지는 ${MAX_LENGTH}자까지 보낼 수 있습니다.`);
        return;
      }
      // 내 말풍선은 여기서 넣지 않는다. 서버가 저장한 메시지를 MESSAGE 로 다시 보내 준다
      if (!socketRef.current?.send(content)) {
        setError("연결이 끊어졌습니다. 잠시 후 다시 보내 주세요.");
        return;
      }
      sentRef.current.push(content);
      setError(null);
      setInput("");
    },
    [input],
  );

  async function handleClose() {
    if (closing) return;
    setClosing(true);
    setError(null);
    try {
      const { status, body } = await closeChat(chatId);
      if (status === 200 && body?.success) {
        router.replace(chatPath("CLOSED", chatId));
        return;
      }
      if (status === 401) {
        router.replace("/login");
        return;
      }
      // 그사이 상담원이 종료했거나 상태가 바뀐 경우: 지금 상태에 맞는 화면으로
      const detail = await getChat(chatId);
      const current = detail.body?.data?.consultationStatus;
      if (current && current !== "IN_PROGRESS") {
        router.replace(chatPath(current, chatId));
        return;
      }
      setError(body?.message ?? "상담을 종료하지 못했습니다. 다시 시도해주세요.");
    } catch {
      setError("서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.");
    }
    setClosing(false);
  }

  if (!chatId) return null;

  return (
    <div className="chat-page">
      <div className="chat-shell">
        <div className="chat-top">
          <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
            <div className="avatar" style={{ width: 40, height: 40 }}>
              <Icon name="user" />
            </div>
            <div className="stack">
              <span className="bold" style={{ fontSize: 16 }}>상담원 {agentName}</span>
              <span style={{ fontSize: 12, color: connected ? "var(--ok)" : "var(--muted)" }}>
                {connected ? "● 상담 중" : "○ 연결 중…"}
              </span>
            </div>
          </div>
          <button type="button" className="btn btn-sm" onClick={handleClose} disabled={closing}>
            {closing ? "종료하는 중..." : "상담 종료"}
          </button>
        </div>

        {connected === false && (
          <div className="alert alert-warn" style={{ borderRadius: 0 }}>
            <Icon name="warn" size={18} />
            <span>연결이 끊어졌습니다. 다시 연결 중…</span>
          </div>
        )}

        {warning && (
          <div className="alert alert-warn" style={{ borderRadius: 0 }}>
            <Icon name="warn" size={18} />
            <span>
              <b>
                경고 {warning.count}
                {threshold ? ` / ${threshold}` : ""}
              </b>{" "}
              · 한 번 더 폭언이 감지되면 상담이 자동 종료됩니다.
            </span>
          </div>
        )}

        <div className="chat-body" aria-live="polite">
          <div className="sys-note">상담원이 연결되었습니다 · 이전 AI 상담 내용이 전달되었습니다</div>

          {messages.map((m) => {
            if (m.senderType === "CUSTOMER") {
              if (m.abuseStatus === "ABUSIVE") {
                return (
                  <div
                    key={m.messageId}
                    className="stack gap-4"
                    style={{ alignSelf: "flex-end", maxWidth: "70%", alignItems: "flex-end" }}
                  >
                    <div className="bubble-me" style={{ maxWidth: "100%", background: "var(--danger)" }}>
                      {m.original ?? m.content}
                    </div>
                    <span className="small muted" style={{ fontSize: 12 }}>
                      부적절한 표현 감지 · 상담원에게는 가려진 상태로 전달됨
                    </span>
                  </div>
                );
              }
              return (
                <div key={m.messageId} className="bubble-me">
                  {m.original ?? m.content}
                </div>
              );
            }
            if (m.senderType === "SYSTEM") {
              return (
                <div key={m.messageId} className="sys-note">
                  {m.content}
                </div>
              );
            }
            return (
              <div key={m.messageId} className="msg-row">
                <AgentAvatar />
                <div className="bubble" style={{ whiteSpace: "pre-wrap" }}>
                  {m.content}
                </div>
              </div>
            );
          })}

          {warning?.message && (
            <div className="alert alert-warn" style={{ alignItems: "flex-start", border: "1px solid #f0cfa8" }}>
              <Icon name="warn" size={20} />
              <div className="stack gap-4" style={{ flex: 1 }}>
                <b>[시스템 경고] 부적절한 표현이 감지되었습니다</b>
                <span style={{ fontSize: 13 }}>{warning.message}</span>
              </div>
            </div>
          )}
          <div ref={endRef} />
        </div>

        <div className="chat-foot">
          {error && (
            <p role="alert" className="small" style={{ color: "var(--danger)", marginBottom: 8 }}>
              {error}
            </p>
          )}
          <form className="chat-input" onSubmit={send}>
            <label htmlFor="cc-input" className="sr-only">메시지 입력</label>
            <input
              id="cc-input"
              className="input"
              type="text"
              placeholder="메시지를 입력하세요"
              style={{ borderRadius: 10 }}
              value={input}
              onChange={(e) => setInput(e.target.value)}
              maxLength={MAX_LENGTH}
              disabled={!connected}
              autoComplete="off"
            />
            <button type="submit" className="send" aria-label="전송" disabled={!connected || !input.trim()}>
              <Icon name="send" />
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
