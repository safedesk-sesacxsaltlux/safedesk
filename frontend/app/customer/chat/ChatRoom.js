"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import Icon from "../../_components/Icon";
import { chatPath, requestHandoff, sendBotMessage } from "../_lib/chatApi";
import Typewriter from "./Typewriter";

const MAX_LENGTH = 500;

// 시연에서 바로 눌러 볼 수 있는 질문 (FAQ 키워드에 매칭되는 문장)
const QUICK_QUESTIONS = ["수도요금 어떻게 내요?", "지방세 납부 방법 알려주세요", "따릉이 어떻게 이용해요?"];

const GREETING =
  "안녕하세요, 120 다산콜센터 AI 상담입니다. 궁금한 내용을 입력하시거나 아래 자주 묻는 질문을 선택해 주세요.";

function AiAvatar() {
  return <div className="avatar">AI</div>;
}

let nextKey = 0;
const newKey = () => `m${nextKey++}`;

// SCR-005 AI 상담 (CUS-02 메시지 전송, CUS-03 상담원 연결)
export default function ChatRoom() {
  const router = useRouter();
  const chatId = useSearchParams().get("chatId");

  // { key, role: "me" | "bot" | "note", content, typing, handoff }
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState("");
  const [waitingReply, setWaitingReply] = useState(false);
  const [connecting, setConnecting] = useState(false);
  const [error, setError] = useState(null);
  const endRef = useRef(null);

  const scrollToEnd = useCallback(() => {
    endRef.current?.scrollIntoView({ block: "end" });
  }, []);

  // 상담 번호 없이 들어오면 상담 시작부터 하도록 고객 메인으로 보낸다
  useEffect(() => {
    if (!chatId) router.replace("/customer");
  }, [chatId, router]);

  useEffect(scrollToEnd, [messages, waitingReply, scrollToEnd]);

  const addMessage = (message) => setMessages((prev) => [...prev, { key: newKey(), ...message }]);

  const finishTyping = useCallback((key) => {
    setMessages((prev) => prev.map((m) => (m.key === key ? { ...m, typing: false } : m)));
  }, []);

  async function send(text) {
    const content = text.trim();
    if (!content || waitingReply) return;
    if (content.length > MAX_LENGTH) {
      setError(`메시지는 ${MAX_LENGTH}자까지 보낼 수 있습니다.`);
      return;
    }

    setError(null);
    setInput("");
    // 서버 응답을 기다리지 않고 고객 말풍선을 먼저 띄운다
    addMessage({ role: "me", content });
    setWaitingReply(true);

    try {
      const { status, body } = await sendBotMessage(chatId, content);
      const code = body?.errorCode;

      if (status === 200 && body?.success) {
        addMessage({
          role: "bot",
          content: body.data.botMessage.content,
          typing: true,
          handoff: body.data.handoffSuggested,
        });
      } else if (code === "CHAT_NOT_IN_BOT" || code === "CHAT_CLOSED") {
        // AI 응답을 기다리는 중에 상담원 연결·종료가 된 경우. 고객 메시지는 서버에 이미 저장돼 있어서
        // 말풍선은 지우지 않는다 (#71 리뷰). 상태에 맞는 화면으로 보낸다.
        addMessage({ role: "note", content: body.message });
        router.replace(code === "CHAT_CLOSED" ? chatPath("CLOSED", chatId) : chatPath("QUEUED", chatId));
      } else if (code === "INVALID_INPUT") {
        // 서버가 저장하지 않았으므로 말풍선을 거두고 입력을 돌려준다
        setMessages((prev) => prev.slice(0, -1));
        setInput(content);
        setError(body.message);
      } else if (status === 401) {
        router.replace("/login");
      } else if (code === "CHAT_NOT_FOUND" || code === "CHAT_ACCESS_DENIED") {
        router.replace("/customer");
      } else {
        addMessage({ role: "note", content: body?.message ?? "답변을 받지 못했습니다. 다시 보내 주세요." });
      }
    } catch {
      // 15초 타임아웃이나 연결 실패. 서버에는 고객 메시지가 저장됐을 수 있어서 말풍선은 남긴다
      addMessage({ role: "note", content: "응답이 늦어지고 있습니다. 잠시 후 다시 보내 주세요." });
    }
    setWaitingReply(false);
  }

  async function connectAgent() {
    if (connecting) return;
    setConnecting(true);
    setError(null);
    try {
      const { status, body } = await requestHandoff(chatId);
      const code = body?.errorCode;
      if (status === 200 && body?.success) {
        router.push(chatPath(body.data.status, chatId));
        return;
      }
      if (code === "CHAT_ALREADY_ACTIVE") {
        router.push(chatPath("IN_PROGRESS", chatId));
        return;
      }
      if (code === "CHAT_CLOSED") {
        router.push(chatPath("CLOSED", chatId));
        return;
      }
      if (status === 401) {
        router.replace("/login");
        return;
      }
      setError(body?.message ?? "상담원을 연결하지 못했습니다. 다시 시도해주세요.");
    } catch {
      setError("서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.");
    }
    setConnecting(false);
  }

  function handleSubmit(e) {
    e.preventDefault();
    send(input);
  }

  if (!chatId) return null;

  return (
    <div className="chat-page">
      <div className="chat-shell">
        <div className="chat-top">
          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <Link href="/customer" aria-label="뒤로" className="icon-link">
              <Icon name="back" />
            </Link>
            <div className="stack">
              <span className="bold" style={{ fontSize: 16 }}>AI 상담</span>
              <span className="muted" style={{ fontSize: 12 }}>
                FAQ 기반 자동 응답
              </span>
            </div>
          </div>
          <button type="button" className="btn btn-outline-primary btn-sm" onClick={connectAgent} disabled={connecting}>
            <Icon name="user" size={16} />
            <span>{connecting ? "연결 요청 중..." : "상담원 연결"}</span>
          </button>
        </div>

        <div className="chat-body" aria-live="polite">
          <div className="center-note">상담이 시작되었습니다</div>

          <div className="msg-row">
            <AiAvatar />
            <div className="stack gap-8">
              <div className="bubble">{GREETING}</div>
              <div className="chip-row">
                {QUICK_QUESTIONS.map((question) => (
                  <button
                    key={question}
                    type="button"
                    className="chip"
                    onClick={() => send(question)}
                    disabled={waitingReply}
                  >
                    {question}
                  </button>
                ))}
              </div>
            </div>
          </div>

          {messages.map((m) => {
            if (m.role === "me") {
              return <div key={m.key} className="bubble-me">{m.content}</div>;
            }
            if (m.role === "note") {
              return <div key={m.key} className="center-note">{m.content}</div>;
            }
            return (
              <div key={m.key} className="msg-row">
                <AiAvatar />
                <div className="bubble stack gap-12" style={m.handoff ? { padding: 14 } : undefined}>
                  <span style={{ whiteSpace: "pre-wrap" }}>
                    {m.typing ? (
                      <Typewriter text={m.content} onTick={scrollToEnd} onDone={() => finishTyping(m.key)} />
                    ) : (
                      m.content
                    )}
                  </span>
                  {/* 자동 이관 안내: 서버가 상담원 연결을 제안하면 답변이 다 나온 뒤 버튼을 보여준다 */}
                  {m.handoff && !m.typing && (
                    <button
                      type="button"
                      className="btn btn-primary btn-sm"
                      style={{ alignSelf: "flex-start" }}
                      onClick={connectAgent}
                      disabled={connecting}
                    >
                      상담원 연결하기
                    </button>
                  )}
                </div>
              </div>
            );
          })}

          {waitingReply && (
            <div className="msg-row">
              <AiAvatar />
              <div className="bubble muted">
                답변을 준비하고 있어요
                <span className="cursor" />
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
          <form className="chat-input" onSubmit={handleSubmit}>
            <label htmlFor="ai-input" className="sr-only">메시지 입력</label>
            <input
              id="ai-input"
              className="input"
              type="text"
              placeholder="메시지를 입력하세요"
              style={{ borderRadius: 10 }}
              value={input}
              onChange={(e) => setInput(e.target.value)}
              maxLength={MAX_LENGTH}
              disabled={waitingReply}
              autoComplete="off"
            />
            <button type="submit" className="send" aria-label="전송" disabled={waitingReply || !input.trim()}>
              <Icon name="send" />
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
