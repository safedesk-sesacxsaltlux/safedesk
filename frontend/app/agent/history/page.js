import { AgentHeader } from "../../_components/Headers";
import AgentHistoryView from "./AgentHistoryView";

export const metadata = { title: "상담 이력 · SafeDesk 상담원" };

export default function AgentHistoryPage() {
  return (
    <>
      <AgentHeader active="history" />
      <main className="container-wide main" style={{ gap: 18 }}>
        <div className="stack gap-4">
          <h1 style={{ fontSize: 24 }}>내 상담 이력</h1>
          <span className="small muted">내가 응대한 고객만 표시됩니다 · 고객별 최근 상담 기준</span>
        </div>

        <AgentHistoryView />
      </main>
    </>
  );
}
