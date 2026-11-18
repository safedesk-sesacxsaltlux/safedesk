"use client";

import { useLayoutEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Icon from "../_components/Icon";
import { chatPath, startChat } from "./_lib/chatApi";

// CUS-01 상담 시작. 응답에 따라 알맞은 화면으로 보낸다.
export default function StartChatButton() {
  const router = useRouter();
  const [starting, setStarting] = useState(false);
  const [error, setError] = useState(null);

  // Cache Components 는 다른 화면으로 가도 이 화면을 숨겨 두고 상태를 그대로 살린다 (Next.js Activity).
  // 돌아왔을 때 「시작하는 중」으로 잠겨 있지 않도록, 화면이 숨겨질 때 버튼 상태를 되돌린다.
  useLayoutEffect(() => {
    return () => {
      setStarting(false);
      setError(null);
    };
  }, []);

  async function handleClick() {
    setStarting(true);
    setError(null);
    try {
      const { status, body } = await startChat();
      const code = body?.errorCode;

      if (status === 201) {
        router.push(chatPath(body.data.status, body.data.chatId));
        return;
      }
      // 이미 진행 중인 상담이 있으면 새로 만들지 않고 그 상담으로 이어간다
      if (code === "CHAT_ALREADY_OPEN") {
        router.push(chatPath(body.data.status, body.data.chatId));
        return;
      }
      // 이용정지 안내 화면은 이 응답으로만 해제 일시·사유를 알 수 있어서 주소에 담아 넘긴다
      if (code === "CUSTOMER_SANCTIONED") {
        const params = new URLSearchParams({
          releaseAt: body.data.releaseAt,
          reason: body.data.reason,
        });
        router.push(`/customer/suspended?${params}`);
        return;
      }
      if (status === 401) {
        router.replace("/login");
        return;
      }
      setError(body?.message ?? "상담을 시작하지 못했습니다. 잠시 후 다시 시도해주세요.");
    } catch {
      setError("서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.");
    }
    setStarting(false);
  }

  return (
    <>
      <button
        type="button"
        className="btn btn-primary btn-lg"
        style={{ alignSelf: "flex-start" }}
        onClick={handleClick}
        disabled={starting}
      >
        <Icon name="chat" size={18} />
        <span>{starting ? "시작하는 중..." : "상담 시작"}</span>
      </button>
      {error && (
        <p role="alert" className="small" style={{ color: "var(--danger)" }}>
          {error}
        </p>
      )}
    </>
  );
}
