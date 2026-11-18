import { AdminHeader } from "../_components/Headers";
import DashboardView from "./DashboardView";

export const metadata = { title: "운영 현황 · SafeDesk 관리자" };

// "검토가 필요한 고객 [N]명" 알림은 쓰지 않는다. (policy.md: "검토 필요" 상태를 쓰지 않기로 함)
export default function AdminDashboardPage() {
  return (
    <>
      <AdminHeader active="dashboard" />
      <main className="container-wide main">
        <DashboardView />
      </main>
    </>
  );
}
