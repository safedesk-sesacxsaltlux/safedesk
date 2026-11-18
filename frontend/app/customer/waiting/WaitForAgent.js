"use client";

import { useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { chatPath, getChat } from "../_lib/chatApi";
import { openChatSocket } from "../../agent/_lib/chatSocket";

// SCR-006 상담원이 수락하면 채팅 화면으로 자동 이동 (CONSULTATION_ACCEPTED). 화면에 그리는 것은 없다.
export default function WaitForAgent() {
  const router = useRouter();
  const chatId = useSearchParams().get("chatId");

  useEffect(() => {
    if (!chatId) return;

    // 지금 상태를 확인해서 대기 중이 아니면 맞는 화면으로 보낸다
    async function checkStatus() {
      const { body } = await getChat(chatId);
      const status = body?.data?.consultationStatus;
      if (body?.success && status !== "QUEUED") {
        router.replace(chatPath(status, chatId));
      }
    }

    // 들어오기 전에 이미 수락됐을 수 있어서 한 번 확인한다
    checkStatus().catch(() => {});

    const socket = openChatSocket(chatId, {
      onEvent: (event) => {
        if (event.type === "CONSULTATION_ACCEPTED") {
          router.replace(chatPath("IN_PROGRESS", chatId));
        }
      },
      // 구독하기 직전에 수락되면 이벤트를 놓치므로, 연결될 때마다 다시 확인한다
      onConnected: () => checkStatus().catch(() => {}),
    });

    // 화면을 떠나거나 숨겨지면(Cache Components) 연결을 끊는다
    return () => socket.close();
  }, [chatId, router]);

  return null;
}
