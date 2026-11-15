// 로그인 정보 저장 위치(localStorage)와 권한별 첫 화면을 한 곳에서 관리한다.

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
