import Link from "next/link";
import { CustomerHeader } from "../_components/Headers";
import ChatAvailability from "./ChatAvailability";
import MyStatusCard from "./MyStatusCard";
import RecentChats from "./RecentChats";
import StartChatButton from "./StartChatButton";

export const metadata = { title: "고객 메인 · SafeDesk" };

export default function CustomerHome() {
  return (
    <>
      <CustomerHeader active="home" />
      <main className="container main" style={{ maxWidth: 1160, paddingTop: 40, gap: 24 }}>
        <h1 style={{ fontSize: 28 }}>무엇을 도와드릴까요?</h1>

        <div className="row">
          <section className="card stack gap-16" style={{ flex: "2 1 480px", padding: 32, borderRadius: 16 }}>
            <ChatAvailability />
            <h2 style={{ fontSize: 22 }}>새 상담 시작하기</h2>
            <p className="muted" style={{ fontSize: 15, lineHeight: 1.7 }}>
              AI 상담원이 먼저 답변을 드립니다. 해결되지 않으면 언제든 ‘상담원
              연결’을 눌러 주세요.
            </p>
            <StartChatButton />
          </section>

          <MyStatusCard />
        </div>

        <section className="card stack gap-12" style={{ padding: 28, borderRadius: 16 }}>
          <div className="between">
            <h2>최근 상담</h2>
            <Link href="/customer/history" style={{ fontSize: 14 }}>
              전체 보기
            </Link>
          </div>
          <RecentChats />
        </section>
      </main>
    </>
  );
}
