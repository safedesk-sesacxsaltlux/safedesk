"use client";

import { useRouter } from "next/navigation";
import { clearLogin, getAccessToken, refreshAccessToken } from "../_lib/auth";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL;

// credentials: "include"가 있어야 리프레시 토큰 쿠키가 함께 가서 서버가 지울 수 있다.
function callLogout(token) {
  return fetch(`${API_BASE_URL}/api/auth/logout`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` },
    credentials: "include",
  });
}

// 서버에 로그아웃을 알려 액세스 토큰은 블랙리스트에 올리고 리프레시 토큰은 삭제한 뒤, 저장된 로그인 정보를 지운다.
// (auth.md API-AUTH-04)
// 액세스 토큰이 만료됐으면(401) 재발급 후 한 번 더 요청한다. 리프레시 토큰 쿠키는 HttpOnly라서
// 프론트가 지울 수 없고, 서버 로그아웃이 성공해야 지워지기 때문이다.
// 그래도 실패하면(서버 연결 실패 등) 사용자는 로그아웃되어야 하므로 결과와 상관없이 정리한다.
export default function LogoutButton() {
  const router = useRouter();

  async function handleClick(e) {
    e.preventDefault();
    try {
      const token = getAccessToken();
      const res = token ? await callLogout(token) : null;
      if (!res || res.status === 401) {
        if ((await refreshAccessToken()) === "ok") {
          await callLogout(getAccessToken());
        }
      }
    } catch {
      // 서버에 닿지 않아도 아래에서 로그인 정보를 지운다
    }
    clearLogin();
    router.replace("/login");
  }

  // 기존 헤더 스타일(a.logout)을 그대로 쓰기 위해 링크 모양을 유지한다
  return (
    <a href="/login" className="logout" onClick={handleClick}>
      로그아웃
    </a>
  );
}
