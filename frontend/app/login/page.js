"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { PublicHeader } from "../_components/Headers";
import { HOME_BY_ROLE, saveLogin } from "../_lib/auth";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL;

export default function LoginPage() {
  const router = useRouter();
  const [form, setForm] = useState({ loginId: "", password: "" });
  const [submitted, setSubmitted] = useState(false);
  const [submitError, setSubmitError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  function handleChange(e) {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
    setSubmitError("");
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setSubmitted(true);
    setSubmitError("");

    if (!form.loginId.trim() || !form.password) return;

    setSubmitting(true);
    try {
      // credentials: "include"가 있어야 응답의 리프레시 토큰 쿠키(Set-Cookie)가 저장된다
      const res = await fetch(`${API_BASE_URL}/api/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({
          loginId: form.loginId.trim(),
          password: form.password,
        }),
      });
      const body = await res.json().catch(() => null);
      if (!res.ok || !body?.success) {
        setSubmitError(body?.message ?? "로그인에 실패했습니다.");
        return;
      }

      const { accessToken, member } = body.data;
      saveLogin(accessToken, member);
      router.push(HOME_BY_ROLE[member.role] ?? "/");
    } catch {
      setSubmitError("서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.");
    } finally {
      setSubmitting(false);
    }
  }

  const loginIdEmpty = submitted && !form.loginId.trim();
  const passwordEmpty = submitted && !form.password;

  return (
    <>
      <title>로그인 · SafeDesk</title>
      <PublicHeader />
      <main className="center-page">
        <form className="auth-card" onSubmit={handleSubmit} noValidate>
          <div className="stack gap-8">
            <h1>로그인</h1>
            <p className="muted small">
              고객 · 상담원 · 관리자 공통 로그인입니다. 권한에 맞는 화면으로
              이동합니다.
            </p>
          </div>

          <div className="field">
            <label htmlFor="lg-id">아이디</label>
            <input
              id="lg-id"
              name="loginId"
              className={`input${loginIdEmpty ? " err" : ""}`}
              type="text"
              placeholder="아이디를 입력하세요"
              autoComplete="username"
              value={form.loginId}
              onChange={handleChange}
            />
            {loginIdEmpty && (
              <span className="msg-err">아이디를 입력해주세요.</span>
            )}
          </div>
          <div className="field">
            <label htmlFor="lg-pw">비밀번호</label>
            <input
              id="lg-pw"
              name="password"
              className={`input${passwordEmpty || submitError ? " err" : ""}`}
              type="password"
              placeholder="비밀번호를 입력하세요"
              autoComplete="current-password"
              value={form.password}
              onChange={handleChange}
            />
            {passwordEmpty && (
              <span className="msg-err">비밀번호를 입력해주세요.</span>
            )}
            {submitError && <span className="msg-err">{submitError}</span>}
          </div>

          <button
            type="submit"
            className="btn btn-primary btn-lg"
            disabled={submitting}
          >
            {submitting ? "로그인 중..." : "로그인"}
          </button>
          <div
            className="small muted"
            style={{
              display: "flex",
              justifyContent: "center",
              gap: 6,
              fontSize: 14,
            }}
          >
            <span>처음 이용하시나요?</span>
            <Link href="/signup">회원가입</Link>
          </div>
        </form>
      </main>
    </>
  );
}
