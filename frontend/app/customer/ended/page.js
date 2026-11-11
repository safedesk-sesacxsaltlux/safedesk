import Link from "next/link";
import Icon from "../../_components/Icon";

export const metadata = { title: "상담 종료 · SafeDesk" };

export default function ChatEndedPage() {
  return (
    <main className="container" style={{ maxWidth: 1040, padding: "48px 24px" }}>
      <div className="row" style={{ gap: 24 }}>
        <section className="card stack gap-20" style={{ flex: "3 1 460px", padding: 36, borderRadius: 16 }}>
          <span className="badge badge-danger bold" style={{ alignSelf: "flex-start", padding: "4px 12px", fontSize: 13 }}>
            TERMINATED · 자동 종료
          </span>
          <div
            style={{
              width: 64, height: 64, borderRadius: "50%", background: "var(--danger-soft)",
              color: "var(--danger)", display: "flex", alignItems: "center", justifyContent: "center",
            }}
          >
            <Icon name="ban" size={30} />
          </div>
          <div className="stack gap-8">
            <h1 style={{ fontSize: 26 }}>상담이 자동 종료되었습니다</h1>
            <p className="muted" style={{ fontSize: 15, lineHeight: 1.7 }}>
              같은 상담에서 부적절한 표현이 2회 감지되어 상담원 보호 정책에 따라
              상담이 종료되었습니다.
            </p>
          </div>

          <div style={{ border: "1px solid var(--line)", borderRadius: 12, overflow: "hidden" }}>
            {[
              ["종료 일시", "YYYY.MM.DD HH:MM"],
              ["종료 사유", "반복 폭언 (경고 2회)"],
              ["누적 위반", "1회"],
            ].map(([k, v]) => (
              <div key={k} className="kv" style={{ padding: "14px 18px", fontSize: 15 }}>
                <span>{k}</span>
                <span style={k === "누적 위반" ? { fontWeight: 700 } : undefined}>{v}</span>
              </div>
            ))}
            <div className="kv" style={{ padding: "14px 18px", fontSize: 15, background: "var(--danger-soft)" }}>
              <span style={{ color: "var(--danger-text)" }}>적용 제재</span>
              <span className="bold" style={{ color: "var(--danger)" }}>7일 이용정지 (~ YYYY.MM.DD)</span>
            </div>
          </div>

          <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
            <Link href="/customer/suspended" className="btn btn-primary" style={{ fontWeight: 500 }}>
              이용정지 안내 보기
            </Link>
            <Link href="/customer" className="btn">홈으로</Link>
          </div>
        </section>

        <section className="card stack gap-12" style={{ flex: "2 1 320px", padding: 28, borderRadius: 16, borderStyle: "dashed", borderColor: "#b8bec8" }}>
          <span className="small bold muted" style={{ fontSize: 12 }}>상태 변형 · 정상 종료</span>
          <span className="badge badge-ok bold" style={{ alignSelf: "flex-start", padding: "4px 12px", fontSize: 13 }}>
            CLOSED
          </span>
          <h2 style={{ fontSize: 20 }}>상담이 종료되었습니다</h2>
          <p className="muted" style={{ fontSize: 14, lineHeight: 1.6 }}>이용해 주셔서 감사합니다.</p>
          <div className="transcript">
            <span className="bold">상담 요약</span>
            <span className="muted" style={{ fontSize: 14 }}>
              [규칙 기반 템플릿 요약 — 문의 유형 / 처리 내용 / 처리 결과]
            </span>
          </div>
          <Link href="/customer" className="btn btn-sm" style={{ alignSelf: "flex-start", minHeight: 44 }}>
            홈으로
          </Link>
        </section>
      </div>
    </main>
  );
}
