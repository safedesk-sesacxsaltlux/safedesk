import Link from "next/link";
import Icon from "../../_components/Icon";
import { PublicHeader } from "../../_components/Headers";

export const metadata = { title: "이용정지 안내 · SafeDesk" };

export default function SuspendedPage() {
  return (
    <>
      <PublicHeader>
        <div className="hdr-right">
          <span>[고객명] 님</span>
          <Link href="/login" className="logout" style={{ display: "inline-flex", alignItems: "center", minHeight: 44, padding: "0 10px", color: "var(--muted)", textDecoration: "none" }}>
            로그아웃
          </Link>
        </div>
      </PublicHeader>

      <main className="container main" style={{ maxWidth: 720, paddingTop: 40 }}>
        <section className="card stack gap-20" style={{ padding: 36, borderRadius: 16, alignItems: "center", textAlign: "center" }}>
          <div
            style={{
              width: 64, height: 64, borderRadius: "50%", background: "var(--danger-soft)",
              color: "var(--danger)", display: "flex", alignItems: "center", justifyContent: "center",
            }}
          >
            <Icon name="lock" size={28} />
          </div>
          <h1 style={{ fontSize: 26 }}>상담 이용이 제한되어 있습니다</h1>
          <p className="muted" style={{ fontSize: 15, lineHeight: 1.7 }}>
            상담 중 폭언으로 상담이 강제 종료되어, 상담원 보호 정책에 따라 일정
            기간 새 상담을 시작할 수 없습니다.
          </p>

          <div style={{ display: "flex", gap: 12, flexWrap: "wrap", justifyContent: "center" }}>
            <div className="wait-stat" style={{ background: "var(--danger-soft)", borderColor: "#f2c4be" }}>
              <span>해제 예정일</span>
              <b>YYYY.MM.DD</b>
            </div>
            <div className="wait-stat">
              <span>남은 기간</span>
              <b>D-[N]</b>
            </div>
          </div>

          <div style={{ width: "100%", textAlign: "left" }}>
            <div className="kv"><span>제재 유형</span><span>7일 이용정지</span></div>
            <div className="kv"><span>제재 기간</span><span>YYYY.MM.DD ~ YYYY.MM.DD</span></div>
            <div className="kv"><span>사유</span><span>상담 중 반복 폭언 (자동 종료)</span></div>
            <div className="kv"><span>누적 위반</span><span>1회</span></div>
          </div>

          <button type="button" className="btn btn-lg" disabled style={{ width: "100%" }}>
            상담 시작 (이용정지 중)
          </button>
          <span className="small muted">
            이의가 있으시면 [이의 제기 채널/연락처]로 문의해 주세요.
          </span>
        </section>

        <section className="card stack gap-12" style={{ borderRadius: 16 }}>
          <h2>제재 기준 안내</h2>
          <div className="table-wrap">
            <table>
              <tbody>
                <tr><td style={{ borderTop: 0 }}>누적 위반 1회</td><td style={{ borderTop: 0 }}>7일 이용정지</td></tr>
                <tr><td>누적 위반 2회</td><td>30일 이용정지</td></tr>
                <tr><td>누적 위반 3회 이상</td><td>30일 이용정지 및 관리자 검토</td></tr>
              </tbody>
            </table>
          </div>
          <Link href="/customer" style={{ fontSize: 14 }}>홈으로 돌아가기</Link>
        </section>
      </main>
    </>
  );
}
