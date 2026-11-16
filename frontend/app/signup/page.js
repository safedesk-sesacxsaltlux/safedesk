"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { PublicHeader } from "../_components/Headers";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL;

const LOGIN_ID_PATTERN = /^[a-z0-9]{4,20}$/;

function validateLoginId(loginId) {
  if (!loginId) return "아이디를 입력해주세요.";
  if (!LOGIN_ID_PATTERN.test(loginId)) return "영문 소문자·숫자 4~20자로 입력해주세요.";
  return "";
}

function validatePassword(password) {
  if (!password) return "비밀번호를 입력해주세요.";
  if (password.length < 8 || password.length > 20) return "비밀번호는 8~20자로 입력해주세요.";
  if (!/[A-Za-z]/.test(password) || !/[0-9]/.test(password) || !/[^A-Za-z0-9]/.test(password)) {
    return "영문·숫자·특수문자를 모두 포함해주세요.";
  }
  return "";
}

function validatePasswordConfirm(password, passwordConfirm) {
  if (!passwordConfirm) return "비밀번호를 한 번 더 입력해주세요.";
  if (password !== passwordConfirm) return "비밀번호가 일치하지 않습니다.";
  return "";
}

function validateName(name) {
  const trimmed = name.trim();
  if (!trimmed) return "이름을 입력해주세요.";
  if (trimmed.length > 20) return "이름은 20자 이하로 입력해주세요.";
  return "";
}

function validateAll(form) {
  return {
    loginId: validateLoginId(form.loginId),
    password: validatePassword(form.password),
    passwordConfirm: validatePasswordConfirm(form.password, form.passwordConfirm),
    name: validateName(form.name),
  };
}

const INITIAL_FORM = { loginId: "", password: "", passwordConfirm: "", name: "" };

export default function SignupPage() {
  const router = useRouter();
  const [form, setForm] = useState(INITIAL_FORM);
  const [touched, setTouched] = useState({});
  const [policyAgreed, setPolicyAgreed] = useState(false);
  const [idCheck, setIdCheck] = useState({ status: "idle", checkedId: "", message: "" });
  const [submitError, setSubmitError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const errors = validateAll(form);
  const idChecked = idCheck.status === "available" && idCheck.checkedId === form.loginId;

  function handleChange(e) {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
    setSubmitError("");
  }

  function handleBlur(e) {
    const { name } = e.target;
    setTouched((prev) => ({ ...prev, [name]: true }));
  }

  async function handleCheckLoginId() {
    setTouched((prev) => ({ ...prev, loginId: true }));
    if (errors.loginId) return;

    setIdCheck({ status: "checking", checkedId: "", message: "" });
    try {
      const res = await fetch(
        `${API_BASE_URL}/api/auth/check-login-id?loginId=${encodeURIComponent(form.loginId)}`,
      );
      const body = await res.json();
      if (!res.ok || !body.success) {
        setIdCheck({ status: "error", checkedId: "", message: body.message ?? "입력 값을 확인해주세요." });
        return;
      }
      setIdCheck({
        status: body.data.available ? "available" : "taken",
        checkedId: form.loginId,
        message: body.message,
      });
    } catch {
      setIdCheck({ status: "error", checkedId: "", message: "서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요." });
    }
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setTouched({ loginId: true, password: true, passwordConfirm: true, name: true });
    setSubmitError("");

    if (Object.values(errors).some(Boolean)) return;
    if (!idChecked) {
      setSubmitError("아이디 중복 확인을 해주세요.");
      return;
    }
    if (!policyAgreed) {
      setSubmitError("상담원 보호 정책에 동의해주세요.");
      return;
    }

    setSubmitting(true);
    try {
      const res = await fetch(`${API_BASE_URL}/api/auth/signup`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          loginId: form.loginId,
          password: form.password,
          passwordConfirm: form.passwordConfirm,
          name: form.name.trim(),
          policyAgreed,
        }),
      });
      const body = await res.json();
      if (!res.ok || !body.success) {
        if (res.status === 409) {
          setIdCheck({ status: "taken", checkedId: form.loginId, message: body.message });
        }
        setSubmitError(body.message ?? "회원가입에 실패했습니다.");
        return;
      }
      alert(body.message ?? "회원가입이 완료되었습니다.");
      router.push("/login");
    } catch {
      setSubmitError("서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.");
    } finally {
      setSubmitting(false);
    }
  }

  function renderIdMessage() {
    if (touched.loginId && errors.loginId) return <span className="msg-err">{errors.loginId}</span>;
    if (idCheck.status === "error") return <span className="msg-err">{idCheck.message}</span>;
    if (idCheck.checkedId !== form.loginId) return null;
    if (idCheck.status === "available") return <span className="msg-ok">{idCheck.message}</span>;
    if (idCheck.status === "taken") return <span className="msg-err">{idCheck.message}</span>;
    return null;
  }

  const showError = (field) => touched[field] && errors[field];

  return (
    <>
      <title>회원가입 · SafeDesk</title>
      <PublicHeader />
      <main className="center-page">
        <form
          className="auth-card"
          style={{ maxWidth: 460, gap: 16 }}
          onSubmit={handleSubmit}
          noValidate
        >
          <div className="stack gap-8">
            <h1>회원가입</h1>
            <p className="muted small">고객 계정만 가입할 수 있습니다.</p>
          </div>

          <div className="field">
            <label htmlFor="su-id">아이디</label>
            <div className="date-pair" style={{ gap: 8 }}>
              <input
                id="su-id"
                name="loginId"
                className={`input${showError("loginId") ? " err" : ""}`}
                type="text"
                placeholder="영문 소문자·숫자 4~20자"
                autoComplete="username"
                value={form.loginId}
                onChange={handleChange}
                onBlur={handleBlur}
              />
              <button
                type="button"
                className="btn"
                style={{ minHeight: 48, padding: "0 14px", fontSize: 14, whiteSpace: "nowrap" }}
                onClick={handleCheckLoginId}
                disabled={idCheck.status === "checking"}
              >
                {idCheck.status === "checking" ? "확인 중..." : "중복 확인"}
              </button>
            </div>
            {renderIdMessage()}
          </div>

          <div className="field">
            <label htmlFor="su-pw">비밀번호</label>
            <input
              id="su-pw"
              name="password"
              className={`input${showError("password") ? " err" : ""}`}
              type="password"
              placeholder="8~20자, 영문·숫자·특수문자 포함"
              autoComplete="new-password"
              value={form.password}
              onChange={handleChange}
              onBlur={handleBlur}
            />
            {showError("password") && <span className="msg-err">{errors.password}</span>}
          </div>

          <div className="field">
            <label htmlFor="su-pw2">비밀번호 확인</label>
            <input
              id="su-pw2"
              name="passwordConfirm"
              className={`input${showError("passwordConfirm") ? " err" : ""}`}
              type="password"
              placeholder="비밀번호를 다시 입력하세요"
              autoComplete="new-password"
              value={form.passwordConfirm}
              onChange={handleChange}
              onBlur={handleBlur}
            />
            {showError("passwordConfirm") && <span className="msg-err">{errors.passwordConfirm}</span>}
          </div>

          <div className="field">
            <label htmlFor="su-name">이름</label>
            <input
              id="su-name"
              name="name"
              className={`input${showError("name") ? " err" : ""}`}
              type="text"
              placeholder="이름"
              autoComplete="name"
              value={form.name}
              onChange={handleChange}
              onBlur={handleBlur}
            />
            {showError("name") && <span className="msg-err">{errors.name}</span>}
          </div>

          <div className="agree-box">
            <input
              id="su-agree"
              type="checkbox"
              checked={policyAgreed}
              onChange={(e) => {
                setPolicyAgreed(e.target.checked);
                setSubmitError("");
              }}
            />
            <label htmlFor="su-agree">
              (필수) 상담원 보호 정책에 동의합니다. 상담 중 폭언 시
              경고·상담 종료·이용정지가 적용될 수 있으며, 원문은 법적 대응을 위해
              보관됩니다.
            </label>
          </div>

          {submitError && <span className="msg-err">{submitError}</span>}

          <button type="submit" className="btn btn-primary btn-lg" disabled={submitting}>
            {submitting ? "가입 처리 중..." : "가입하기"}
          </button>
          <div className="muted" style={{ display: "flex", justifyContent: "center", gap: 6, fontSize: 14 }}>
            <span>이미 계정이 있으신가요?</span>
            <Link href="/login">로그인</Link>
          </div>
        </form>
      </main>
    </>
  );
}
