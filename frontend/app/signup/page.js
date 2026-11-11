import Link from "next/link";
import { PublicHeader } from "../_components/Headers";

export const metadata = { title: "회원가입 · SafeDesk" };

export default function SignupPage() {
  return (
    <>
      <PublicHeader />
      <main className="center-page">
        <div className="auth-card" style={{ maxWidth: 460, gap: 16 }}>
          <div className="stack gap-8">
            <h1>회원가입</h1>
            <p className="muted small">고객 계정만 가입할 수 있습니다.</p>
          </div>

          <div className="field">
            <label htmlFor="su-id">아이디</label>
            <div className="date-pair" style={{ gap: 8 }}>
              <input id="su-id" className="input" type="text" placeholder="영문·숫자 4~20자" />
              <button type="button" className="btn" style={{ minHeight: 48, padding: "0 14px", fontSize: 14, whiteSpace: "nowrap" }}>
                중복 확인
              </button>
            </div>
            <span className="msg-ok">사용 가능한 아이디입니다.</span>
          </div>
          <div className="field">
            <label htmlFor="su-pw">비밀번호</label>
            <input id="su-pw" className="input" type="password" placeholder="8자 이상, 영문·숫자·특수문자 포함" />
          </div>
          <div className="field">
            <label htmlFor="su-pw2">비밀번호 확인</label>
            <input id="su-pw2" className="input err" type="password" placeholder="비밀번호를 다시 입력하세요" />
            <span className="msg-err">비밀번호가 일치하지 않습니다.</span>
          </div>
          <div className="field">
            <label htmlFor="su-name">이름</label>
            <input id="su-name" className="input" type="text" placeholder="이름" />
          </div>

          <div className="agree-box">
            <input id="su-agree" type="checkbox" />
            <label htmlFor="su-agree">
              (필수) 상담원 보호 정책에 동의합니다. 상담 중 폭언 시
              경고·상담 종료·이용정지가 적용될 수 있으며, 원문은 법적 대응을 위해
              보관됩니다.
            </label>
          </div>

          <Link href="/login" className="btn btn-primary btn-lg">
            가입하기
          </Link>
          <div className="muted" style={{ display: "flex", justifyContent: "center", gap: 6, fontSize: 14 }}>
            <span>이미 계정이 있으신가요?</span>
            <Link href="/login">로그인</Link>
          </div>
        </div>
      </main>
    </>
  );
}
