// 로그인 정보 저장 위치와 권한별 첫 화면, 토큰 재발급을 한 곳에서 관리한다. (auth.md 토큰 정책)

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL;

const ACCESS_TOKEN_KEY = "accessToken";
const MEMBER_KEY = "member";

// 로그인 후 권한별 첫 화면 (auth.md API-AUTH-03)
export const HOME_BY_ROLE = {
  CUSTOMER: "/customer",
  AGENT: "/agent",
  ADMIN: "/admin",
};

export function saveLogin(accessToken, member) {
  localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
  localStorage.setItem(MEMBER_KEY, JSON.stringify(member));
}

export function getAccessToken() {
  return localStorage.getItem(ACCESS_TOKEN_KEY);
}

export function clearLogin() {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(MEMBER_KEY);
}

let refreshing = null;

// 리프레시 토큰 쿠키로 새 액세스 토큰을 받는다. (API-AUTH-06)
// 결과: "ok" | "unauthorized"(다시 로그인 필요) | "error"(서버 연결 실패)
//
// 리프레시 토큰은 한 번 쓰면 바뀌므로, 같은 쿠키로 요청이 두 번 나가면 두 번째는 실패하고 쿠키까지 지워진다.
// 개발 모드의 React Strict Mode는 useEffect를 두 번 실행하므로, 진행 중인 요청이 있으면 그 결과를 같이 쓴다.
export function refreshAccessToken() {
  if (!refreshing) {
    refreshing = (async () => {
      try {
        const res = await fetch(`${API_BASE_URL}/api/auth/refresh`, {
          method: "POST",
          credentials: "include",
        });
        const body = await res.json().catch(() => null);
        if (!res.ok || !body?.success) {
          clearLogin();
          return "unauthorized";
        }
        saveLogin(body.data.accessToken, body.data.member);
        return "ok";
      } catch {
        return "error";
      } finally {
        refreshing = null;
      }
    })();
  }
  return refreshing;
}
