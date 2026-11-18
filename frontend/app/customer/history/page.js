import { CustomerHeader } from "../../_components/Headers";
import CustomerHistoryView from "./CustomerHistoryView";

export const metadata = { title: "내 상담 이력 · SafeDesk" };

export default function MyHistoryPage() {
  return (
    <>
      <CustomerHeader active="history" />
      <main className="container main" style={{ maxWidth: 860, paddingTop: 36 }}>
        <div className="stack gap-8">
          <h1 style={{ fontSize: 26 }}>내 상담 이력</h1>
          <p className="muted small">본인의 상담 기록만 표시됩니다.</p>
        </div>

        <CustomerHistoryView />
      </main>
    </>
  );
}
