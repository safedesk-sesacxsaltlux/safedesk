import { Client } from "@stomp/stompjs";
import { getAccessToken, refreshAccessToken } from "../../_lib/auth";

const WS_URL = process.env.NEXT_PUBLIC_WS_URL;

// 실시간 채팅 연결 (contract.md WebSocket, chat.md API-005).
// - CONNECT 프레임 헤더에 Authorization: Bearer {token} (브라우저 WebSocket은 HTTP 헤더를 못 붙임)
// - SUBSCRIBE /topic/chats/{chatId}: MESSAGE·WARNING_ISSUED·CONSULTATION_* 를 type으로 구분
// - SUBSCRIBE /user/queue/errors: 내가 보낸 메시지가 거부됐을 때 (예: CHAT_CLOSED)
// - 끊기면 3초 뒤 다시 연결한다. 연결될 때마다 onConnected가 불리므로 거기서 이력을 다시 불러온다 (NFR-008)
export function openChatSocket(chatId, { onEvent, onError, onConnected, onDisconnected }) {
  const client = new Client({
    brokerURL: WS_URL,
    reconnectDelay: 3000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    // 다시 연결할 때마다 최신 토큰을 넣는다
    beforeConnect: async () => {
      client.connectHeaders = { Authorization: `Bearer ${getAccessToken()}` };
    },
  });

  client.onConnect = () => {
    client.subscribe(`/topic/chats/${chatId}`, (frame) => {
      const payload = parse(frame.body);
      if (payload) onEvent?.(payload);
    });
    client.subscribe("/user/queue/errors", async (frame) => {
      const payload = parse(frame.body);
      if (!payload) return;
      if (payload.errorCode === "UNAUTHORIZED") {
        // CONNECT 뒤에 토큰이 만료되거나 로그아웃되면 서버가 SEND·SUBSCRIBE를 거부한다.
        // 재발급되면 새 토큰으로 다시 연결하고, 안 되면 로그인 화면으로 보낸다.
        const refreshed = await refreshAccessToken();
        if (refreshed === "ok") {
          await client.deactivate();
          client.activate();
          onError?.({ ...payload, errorCode: "RECONNECTING", message: "연결을 새로 맺는 중입니다. 방금 메시지는 다시 보내주세요." });
          return;
        }
        if (refreshed === "error") {
          onError?.({ ...payload, errorCode: "RECONNECTING", message: "서버에 연결하지 못했습니다. 잠시 후 다시 보내주세요." });
          return;
        }
        client.deactivate();
      }
      onError?.(payload);
    });
    onConnected?.();
  };

  // 서버가 CONNECT·SUBSCRIBE를 거부하면 ERROR 프레임이 오고 연결이 끊긴다
  client.onStompError = async (frame) => {
    const code = frame.headers?.message;
    if (code === "UNAUTHORIZED") {
      // 토큰 만료면 재발급해 두고, 다음 재연결 때 새 토큰으로 붙는다
      const refreshed = await refreshAccessToken();
      if (refreshed === "unauthorized") {
        client.deactivate();
        onError?.({ type: "ERROR", errorCode: "UNAUTHORIZED", message: "로그인이 필요합니다." });
      }
      return;
    }
    // 없는 상담도 서버가 CHAT_ACCESS_DENIED 로 돌려준다 (상담 번호 존재 여부를 드러내지 않음)
    if (code === "CHAT_ACCESS_DENIED" || code === "ACCESS_DENIED") {
      client.deactivate();
      onError?.({ type: "ERROR", errorCode: code, message: "이 상담에 접근할 수 없습니다." });
    }
  };

  client.onWebSocketClose = () => onDisconnected?.();

  client.activate();

  return {
    // 연결돼 있을 때만 보낸다. 결과: true(보냄) | false(연결 안 됨)
    send(content) {
      if (!client.connected) return false;
      client.publish({
        destination: `/app/chats/${chatId}/messages`,
        body: JSON.stringify({ content }),
      });
      return true;
    },
    close() {
      client.deactivate();
    },
  };
}

function parse(body) {
  try {
    return JSON.parse(body);
  } catch {
    return null;
  }
}
