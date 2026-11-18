import { AdminHeader } from "../../_components/Headers";
import PolicyForm from "./PolicyForm";

export const metadata = { title: "정책 관리 · SafeDesk 관리자" };

export default function PolicyPage() {
  return (
    <>
      <AdminHeader active="policy" />
      <main className="container-wide main" style={{ maxWidth: 960 }}>
        <div className="stack gap-8">
          <h1 style={{ fontSize: 24 }}>제재 정책 관리</h1>
          <p className="muted small">
            변경한 정책은 저장 이후 시작되는 상담과 새로 발생하는 위반부터 적용됩니다.
          </p>
        </div>

        <PolicyForm />
      </main>
    </>
  );
}
