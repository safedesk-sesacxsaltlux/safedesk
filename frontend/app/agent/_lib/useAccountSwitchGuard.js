"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useMember } from "../../_components/RoleGuard";

// 로그인 정보(accessToken·member)는 localStorage 에 있어서 같은 브라우저의 모든 탭이 함께 쓴다.
// 한 탭에서 상담원 A, 다른 탭에서 상담원 B 로 로그인하면 A 탭도 B 토큰으로 요청하게 되어
// 동시 수락에서 진 탭이 B 의 상담을 「상담 중」으로 보여주는 문제가 생겼다.
// 다른 탭에서 계정이 바뀌거나 로그아웃되면 이 탭은 바로 로그인 화면으로 보낸다.
export function useAccountSwitchGuard() {
  const router = useRouter();
  const me = useMember();

  useEffect(() => {
    if (!me) return;
    function onStorage(e) {
      if (e.key !== "member" && e.key !== "accessToken" && e.key !== null) return;
      let current = null;
      try {
        current = JSON.parse(localStorage.getItem("member") ?? "null");
      } catch {
        current = null;
      }
      if (current?.memberId === me.memberId) return; // 같은 계정의 토큰 재발급은 그대로 둔다
      window.alert("다른 탭에서 다른 계정으로 로그인하거나 로그아웃했습니다. 다시 로그인해주세요.");
      router.replace("/login");
    }
    window.addEventListener("storage", onStorage);
    return () => window.removeEventListener("storage", onStorage);
  }, [me, router]);
}
