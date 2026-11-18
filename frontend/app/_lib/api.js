// 로그인이 필요한 API 를 부르는 공통 함수. (이력·통계 화면에서 사용)
// 액세스 토큰이 만료돼 401 이 오면 리프레시 토큰으로 한 번 재발급한 뒤 다시 요청한다.

import { getAccessToken, refreshAccessToken } from "./auth";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL;

// 결과: { ok: true, data } | { ok: false, status, errorCode, message }
export async function apiGet(path, params) {
  const query = toQuery(params);
  const url = `${API_BASE_URL}${path}${query ? `?${query}` : ""}`;

  try {
    let res = await fetch(url, { headers: { Authorization: `Bearer ${getAccessToken()}` } });
    if (res.status === 401 && (await refreshAccessToken()) === "ok") {
      res = await fetch(url, { headers: { Authorization: `Bearer ${getAccessToken()}` } });
    }
    const body = await res.json().catch(() => null);
    if (!res.ok || !body?.success) {
      return {
        ok: false,
        status: res.status,
        errorCode: body?.errorCode ?? null,
        message: body?.message ?? "요청을 처리하지 못했습니다.",
      };
    }
    return { ok: true, data: body.data };
  } catch {
    return { ok: false, status: 0, errorCode: null, message: "서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요." };
  }
}

// 빈 값(null, undefined, "", "ALL")은 보내지 않는다. → 서버에서 "조건 없음"
function toQuery(params) {
  if (!params) return "";
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value === null || value === undefined || value === "" || value === "ALL") return;
    search.append(key, String(value));
  });
  return search.toString();
}
