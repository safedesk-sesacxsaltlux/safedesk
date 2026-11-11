import Link from "next/link";
import Icon from "../_components/Icon";
import { CustomerHeader } from "../_components/Headers";

export const metadata = { title: "고객 메인 · SafeDesk" };

export default function CustomerHome() {
  return (
    <>
      <CustomerHeader active="home" />
      <main className="container main" style={{ maxWidth: 1160, paddingTop: 40, gap: 24 }}>
        <h1 style={{ fontSize: 28 }}>무엇을 도와드릴까요?</h1>

        <div className="row">
          <section className="card stack gap-16" style={{ flex: "2 1 480px", padding: 32, borderRadius: 16 }}>
            <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
              <span style={{ width: 10, height: 10, borderRadius: "50%", background: "var(--ok)" }} />
              <span style={{ fontSize: 14, color: "var(--ok)", fontWeight: 500 }}>상담 가능</span>
            </div>
            <h2 style={{ fontSize: 22 }}>새 상담 시작하기</h2>
            <p className="muted" style={{ fontSize: 15, lineHeight: 1.7 }}>
              AI 상담원이 먼저 답변을 드립니다. 해결되지 않으면 언제든 ‘상담원
              연결’을 눌러 주세요.
            </p>
            <Link href="/customer/chat" className="btn btn-primary btn-lg" style={{ alignSelf: "flex-start" }}>
              <Icon name="chat" size={18} />
              <span>상담 시작</span>
            </Link>
            <div className="dashed">
              이용정지 상태일 때: 버튼 비활성 + “이용정지 중입니다 (해제 예정일
              YYYY.MM.DD)” 안내 →{" "}
              <Link href="/customer/suspended">이용정지 안내 화면으로 이동</Link>
            </div>
          </section>

          <section className="card stack gap-12" style={{ flex: "1 1 280px", padding: 28, borderRadius: 16 }}>
            <h2>내 이용 상태</h2>
            <div className="kv">
              <span>제재 상태</span>
              <span style={{ fontWeight: 500, color: "var(--ok)" }}>정상</span>
            </div>
            <div className="kv">
              <span>누적 위반</span>
              <span style={{ fontWeight: 500 }}>0회</span>
            </div>
            <p className="muted small" style={{ lineHeight: 1.6 }}>
              상담 중 폭언이 감지되면 1회 경고, 반복 시 상담이 자동 종료되고
              이용이 제한됩니다.
            </p>
          </section>
        </div>

        <section className="card stack gap-12" style={{ padding: 28, borderRadius: 16 }}>
          <div className="between">
            <h2>최근 상담</h2>
            <Link href="/customer/history" style={{ fontSize: 14 }}>
              전체 보기
            </Link>
          </div>
          <div className="table-wrap">
            <table style={{ minWidth: 560 }}>
              <thead>
                <tr>
                  <th>일시</th>
                  <th>문의 요약</th>
                  <th>처리</th>
                  <th>상태</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td>YYYY.MM.DD 14:20</td>
                  <td>[상담 요약 텍스트]</td>
                  <td>AI 응대</td>
                  <td><span className="badge">CLOSED</span></td>
                </tr>
                <tr>
                  <td>YYYY.MM.DD 10:05</td>
                  <td>[상담 요약 텍스트]</td>
                  <td>상담원 연결</td>
                  <td><span className="badge">CLOSED</span></td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </main>
    </>
  );
}
