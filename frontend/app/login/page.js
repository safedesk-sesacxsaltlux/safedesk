import Link from "next/link";
import { PublicHeader } from "../_components/Headers";

export const metadata = { title: "로그인 · SafeDesk" };

export default function LoginPage() {
  return (
    <>
      <PublicHeader />
      <main className="center-page">
        <div className="auth-card">
          <div className="stack gap-8">
            <h1>로그인</h1>
            <p className="muted small">
              고객 · 상담원 · 관리자 공통 로그인입니다. 권한에 맞는 화면으로
              이동합니다.
            </p>
          </div>

          <div className="field">
            <label htmlFor="lg-id">아이디</label>
            <input id="lg-id" className="input" type="text" placeholder="아이디를 입력하세요" />
          </div>
          <div className="field">
            <label htmlFor="lg-pw">비밀번호</label>
            <input id="lg-pw" className="input err" type="password" placeholder="비밀번호를 입력하세요" />
            <span className="msg-err">
              아이디 또는 비밀번호가 일치하지 않습니다. (로그인 실패 상태)
            </span>
          </div>

          <Link href="/customer" className="btn btn-primary btn-lg">
            로그인
          </Link>
          <div className="small muted" style={{ display: "flex", justifyContent: "center", gap: 6, fontSize: 14 }}>
            <span>처음 이용하시나요?</span>
            <Link href="/signup">회원가입</Link>
          </div>

          <div className="stack gap-12" style={{ borderTop: "1px solid var(--line)", paddingTop: 16 }}>
            <span className="small muted">시연용 바로가기 (권한별 이동 결과)</span>
            <div className="demo-links">
              <Link href="/customer" className="btn">고객</Link>
              <Link href="/agent" className="btn">상담원</Link>
              <Link href="/admin" className="btn">관리자</Link>
            </div>
            <span className="small muted" style={{ fontSize: 12 }}>
              상담원 · 관리자 계정은 사전에 등록된 계정을 사용합니다.
            </span>
          </div>
        </div>
      </main>
    </>
  );
}
