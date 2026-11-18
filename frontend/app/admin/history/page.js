import { AdminHeader } from "../../_components/Headers";
import AdminHistoryView from "./AdminHistoryView";

export const metadata = { title: "상담 이력 · SafeDesk 관리자" };

export default function AdminHistoryPage() {
  return (
    <>
      <AdminHeader active="history" />
      <main className="container-wide main">
        <div className="stack gap-4">
          <h1 style={{ fontSize: 24 }}>상담 이력</h1>
          <span className="small muted">종료된 전체 상담 · 폭언 원문은 표시하지 않습니다</span>
        </div>

        <AdminHistoryView />
      </main>
    </>
  );
}
