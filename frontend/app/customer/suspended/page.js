import { Suspense } from "react";
import Link from "next/link";
import { PublicHeader } from "../../_components/Headers";
import LogoutButton from "../../_components/LogoutButton";
import MemberName from "../../_components/MemberName";
import SuspensionNotice from "./SuspensionNotice";

export const metadata = { title: "이용정지 안내 · SafeDesk" };

export default function SuspendedPage() {
  return (
    <>
      <PublicHeader>
        <div className="hdr-right">
          <span>
            <MemberName /> 님
          </span>
          <LogoutButton />
        </div>
      </PublicHeader>

      <main className="container main" style={{ maxWidth: 720, paddingTop: 40 }}>
        {/* 주소의 해제 일시·사유를 읽는 부분만 브라우저에서 그린다 (useSearchParams) */}
        <Suspense fallback={null}>
          <SuspensionNotice />
        </Suspense>

        <section className="card stack gap-12" style={{ borderRadius: 16 }}>
          <h2>제재 기준 안내</h2>
          <div className="table-wrap">
            <table>
              <tbody>
                <tr><td style={{ borderTop: 0 }}>누적 위반 1회</td><td style={{ borderTop: 0 }}>7일 이용정지</td></tr>
                <tr><td>누적 위반 2회</td><td>30일 이용정지</td></tr>
                <tr><td>누적 위반 3회 이상</td><td>30일 이용정지 및 관리자 검토</td></tr>
              </tbody>
            </table>
          </div>
          <Link href="/customer" style={{ fontSize: 14 }}>홈으로 돌아가기</Link>
        </section>
      </main>
    </>
  );
}
