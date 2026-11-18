"use client";

import { useLayoutEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { cancelChat, chatPath } from "../_lib/chatApi";

// SCR-006 대기 취소 (CUS-04)
export default function CancelWaitButton() {
  const router = useRouter();
  const chatId = useSearchParams().get("chatId");
  const [cancelling, setCancelling] = useState(false);
  const [error, setError] = useState(null);

  // Cache Components 가 화면 상태를 살려 두므로, 숨겨질 때 「취소하는 중」 잠금을 푼다
  useLayoutEffect(() => {
    return () => {
      setCancelling(false);
      setError(null);
    };
  }, []);

  async function handleClick() {
    // 상담 번호를 모르면 취소할 상담이 없으므로 고객 메인으로 간다
    if (!chatId) {
      router.push("/customer");
      return;
    }
    setCancelling(true);
    setError(null);
    try {
      const { status, body } = await cancelChat(chatId);
      const code = body?.errorCode;
      // 이미 종료된 상담도 200 이 온다 (명세: 연타해도 에러 아님)
      if (status === 200 && body?.success) {
        router.push("/customer");
        return;
      }
      // 취소 직전에 상담원이 수락했으면 채팅으로 간다
      if (code === "CHAT_ALREADY_ACTIVE") {
        router.push(chatPath("IN_PROGRESS", chatId));
        return;
      }
      if (status === 401) {
        router.replace("/login");
        return;
      }
      if (code === "CHAT_NOT_FOUND" || code === "CHAT_ACCESS_DENIED") {
        router.push("/customer");
        return;
      }
      setError(body?.message ?? "취소하지 못했습니다. 다시 시도해주세요.");
    } catch {
      setError("서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.");
    }
    setCancelling(false);
  }

  return (
    <>
      <button type="button" className="btn" onClick={handleClick} disabled={cancelling}>
        {cancelling ? "취소하는 중..." : "연결 취소"}
      </button>
      {error && (
        <p role="alert" className="small" style={{ color: "var(--danger)", width: "100%", textAlign: "center" }}>
          {error}
        </p>
      )}
    </>
  );
}
