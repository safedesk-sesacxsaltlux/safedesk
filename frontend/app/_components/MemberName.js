"use client";

import { useMember } from "./RoleGuard";

// Headers.js는 서버 컴포넌트라 context를 읽을 수 없어서, 이름 부분만 클라이언트 컴포넌트로 둔다.
export default function MemberName() {
  return useMember()?.name ?? "";
}
