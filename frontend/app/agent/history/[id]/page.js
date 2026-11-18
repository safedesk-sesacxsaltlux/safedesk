import { AgentHeader } from "../../../_components/Headers";
import ConsultationDetail from "../../../_components/ConsultationDetail";

export const metadata = { title: "상담 상세 · SafeDesk 상담원" };

// 상담원은 본인이 맡은 상담만 열 수 있다 (서버가 확인, 아니면 403)
export default async function AgentConsultDetailPage({ params }) {
  const { id } = await params;

  return (
    <>
      <AgentHeader active="history" />
      <main className="container-wide main">
        <ConsultationDetail id={id} backHref="/agent/history" backLabel="내 상담 이력" />
      </main>
    </>
  );
}
