import { AdminHeader } from "../_components/Headers";
import DashboardView from "./DashboardView";

export const metadata = { title: "운영 현황 · SafeDesk 관리자" };

// "검토가 필요한 고객 [N]명" 알림은 기준(누적 위반 3회 이상?)과 API 값이 정해지지 않아 숨겨 두었다. (건웅님 확인 후 추가)
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
