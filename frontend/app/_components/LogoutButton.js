"use client";

import { useRouter } from "next/navigation";
import { clearLogin, getAccessToken } from "../_lib/auth";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL;

// 서버에 로그아웃을 알려 토큰을 블랙리스트에 올리고, 저장된 로그인 정보를 지운다. (auth.md API-AUTH-04)
// 서버 요청이 실패해도(이미 만료된 토큰, 서버 연결 실패) 사용자는 로그아웃되어야 하므로 결과와 상관없이 정리한다.
export default function LogoutButton() {
  const router = useRouter();

  async function handleClick(e) {
    e.preventDefault();
    const token = getAccessToken();
    if (token) {
      try {
        await fetch(`${API_BASE_URL}/api/auth/logout`, {
          method: "POST",
          headers: { Authorization: `Bearer ${token}` },
        });
      } catch {
        // 서버에 닿지 않아도 아래에서 로그인 정보를 지운다
      }
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
