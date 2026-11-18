import { AdminHeader } from "../../../_components/Headers";
import SanctionDetailView from "./SanctionDetailView";

export const metadata = { title: "제재 상세 · SafeDesk 관리자" };

export default async function SanctionDetailPage({ params }) {
  const { id } = await params;

  return (
    <>
      <AdminHeader active="sanctions" />
      <main className="container-wide main">
        <SanctionDetailView id={id} />
      </main>
    </>
  );
}
