import { getAccessToken, refreshAccessToken } from "../../_lib/auth";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL;

// 액세스 토큰이 만료(401)됐으면 재발급 후 한 번 더 요청한다. (RoleGuard·LogoutButton과 같은 방식)
export async function requestWithAuth(path, method = "GET") {
  const send = () =>
    fetch(`${API_BASE_URL}${path}`, {
      method,
      headers: { Authorization: `Bearer ${getAccessToken()}` },
    });
  const res = await send();
  if (res.status !== 401) return res;
  if ((await refreshAccessToken()) !== "ok") return res;
  return send();
}

// 공통 응답(contract.md)을 풀어서 돌려준다.
// 결과: { data } | { error, errorCode, status } | { unauthorized: true }
export async function callApi(path, method = "GET", fallbackMessage = "요청을 처리하지 못했습니다.") {
  try {
    const res = await requestWithAuth(path, method);
    if (res.status === 401) return { unauthorized: true };
    const body = await res.json().catch(() => null);
    if (!res.ok || !body?.success) {
      return {
        error: body?.message ?? fallbackMessage,
        errorCode: body?.errorCode,
        status: res.status,
      };
    }
    return { data: body.data };
  } catch {
    return { error: "서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요." };
  }
}

// "2026-11-18T10:05:00" → "10:05"
export function formatTime(value) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return date.toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit", hour12: false });
}

// 초 → MM:SS
export function formatSeconds(seconds) {
  const safe = Math.max(0, Math.floor(seconds ?? 0));
  const mm = String(Math.floor(safe / 60)).padStart(2, "0");
  const ss = String(safe % 60).padStart(2, "0");
  return `${mm}:${ss}`;
}
