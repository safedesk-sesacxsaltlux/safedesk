// 고객 상담 API(CUS-01~04) 호출을 한 곳에 모은다. (docs/api/customer-chat.md)
import { getAccessToken, refreshAccessToken } from "../../_lib/auth";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL;

// 서버가 AI 를 10초에 끊고 대체 답변을 만들 시간까지 기다린다 (CUS-02 명세: 프론트 타임아웃 15초)
const BOT_MESSAGE_TIMEOUT_MS = 15_000;

// 결과: { status, body } — body 는 공통 응답 { success, data, message, errorCode }
// 액세스 토큰이 만료(401)됐으면 한 번 재발급하고 다시 보낸다 (RoleGuard 와 같은 방식)
async function request(path, { method = "GET", body, timeoutMs } = {}) {
  const send = () => {
    const controller = timeoutMs ? new AbortController() : null;
    const timer = controller ? setTimeout(() => controller.abort(), timeoutMs) : null;
    return fetch(`${API_BASE_URL}${path}`, {
      method,
      headers: {
        Authorization: `Bearer ${getAccessToken()}`,
        ...(body ? { "Content-Type": "application/json" } : {}),
      },
      body: body ? JSON.stringify(body) : undefined,
      signal: controller?.signal,
    }).finally(() => clearTimeout(timer));
  };

  let res = await send();
  if (res.status === 401 && (await refreshAccessToken()) === "ok") {
    res = await send();
  }
  const json = await res.json().catch(() => null);
  return { status: res.status, body: json };
}

// CUS-01 상담 시작
export function startChat() {
  return request("/api/chats", { method: "POST" });
}

// CUS-02 AI 상담 메시지 전송
export function sendBotMessage(chatId, content) {
  return request(`/api/chats/${chatId}/bot-messages`, {
    method: "POST",
    body: { content },
    timeoutMs: BOT_MESSAGE_TIMEOUT_MS,
  });
}

// CUS-03 상담원 연결 요청
export function requestHandoff(chatId) {
  return request(`/api/chats/${chatId}/handoff`, { method: "PATCH" });
}

// 상담 상태별 고객 화면 (contract.md 화면 라우트)
export function chatPath(status, chatId) {
  switch (status) {
    case "AI_CHAT":
      return `/customer/chat?chatId=${chatId}`;
    case "QUEUED":
      return `/customer/waiting?chatId=${chatId}`;
    case "IN_PROGRESS":
      return `/customer/live?chatId=${chatId}`;
    default:
      return `/customer/ended?chatId=${chatId}`;
  }
}
