import { AdminHeader } from "../../../_components/Headers";
import ConsultationDetail from "../../../_components/ConsultationDetail";

export const metadata = { title: "상담 상세 · SafeDesk 관리자" };

// Next.js 16: params 는 Promise 라서 await 로 꺼낸다
export default async function ConsultDetailPage({ params }) {
  const { id } = await params;

  return (
    <>
      <AdminHeader active="history" />
      <main className="container-wide main">
        <ConsultationDetail id={id} backHref="/admin/history" backLabel="상담 이력" showSanctionLink />
      </main>
    </>
  );
}
