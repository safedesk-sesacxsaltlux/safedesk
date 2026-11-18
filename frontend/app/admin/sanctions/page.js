import { AdminHeader } from "../../_components/Headers";
import SanctionsView from "./SanctionsView";

export const metadata = { title: "제재 관리 · SafeDesk 관리자" };

export default function SanctionsPage() {
  return (
    <>
      <AdminHeader active="sanctions" />
      <main className="container-wide main">
        <h1 style={{ fontSize: 24 }}>제재 관리</h1>

        <SanctionsView />
      </main>
    </>
  );
}
