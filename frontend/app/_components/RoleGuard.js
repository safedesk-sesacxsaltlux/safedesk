"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { HOME_BY_ROLE, clearLogin, getAccessToken } from "../_lib/auth";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL;

// 화면에 들어올 때 /api/auth/me로 현재 사용자를 확인하고, 권한이 맞을 때만 화면을 보여준다.
// localStorage의 member는 사용자가 고칠 수 있으므로 서버가 토큰으로 확인한 role만 믿는다.
// 화면 접근만 막는 장치이고, 실제 데이터 보호는 각 API의 서버 권한 검사가 담당한다.
export default function RoleGuard({ role, children }) {
  const router = useRouter();
  const [status, setStatus] = useState("checking"); // checking | allowed | error

  useEffect(() => {
    let cancelled = false;

    async function check() {
      const token = getAccessToken();
      if (!token) {
        router.replace("/login");
        return;
      }

      try {
        const res = await fetch(`${API_BASE_URL}/api/auth/me`, {
          headers: { Authorization: `Bearer ${token}` },
        });
        if (cancelled) return;

        if (res.status === 401) {
          clearLogin();
          router.replace("/login");
          return;
        }

        const body = await res.json();
        if (!res.ok || !body.success) {
          setStatus("error");
          return;
        }

        const myRole = body.data.role;
        if (myRole !== role) {
          router.replace(HOME_BY_ROLE[myRole] ?? "/login");
          return;
        }
        setStatus("allowed");
      } catch {
        if (!cancelled) setStatus("error");
      }
    }

    check();
    return () => {
      cancelled = true;
    };
  }, [role, router]);

  if (status === "error") {
    return (
      <main className="center-page">
        <p className="muted">
          서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.
        </p>
      </main>
    );
  }

  // 확인이 끝나기 전에는 화면 내용을 그리지 않는다 (권한 없는 사람에게 잠깐이라도 보이지 않게)
  if (status !== "allowed") return null;

  return children;
}
