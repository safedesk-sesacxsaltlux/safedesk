import Link from "next/link";
import Icon from "./_components/Icon";
import { PublicHeader } from "./_components/Headers";

export const metadata = { title: "SafeDesk" };

const FEATURES = [
  {
    icon: "bot",
    title: "AI 1차 응대",
    text: "FAQ 기반으로 자주 묻는 질문에 즉시 답변합니다.",
  },
  {
    icon: "lines",
    title: "상담원 연결",
    text: "AI로 해결되지 않으면 대기열을 거쳐 상담원과 실시간으로 대화합니다.",
  },
  {
    icon: "shieldCheck",
    title: "상담원 보호 정책",
    text: "폭언 시 경고 → 상담 자동 종료 → 7일·30일 이용정지가 일관되게 적용됩니다.",
  },
];

export default function Home() {
  return (
    <>
      <PublicHeader>
        <nav className="nav">
          <Link href="/login" className="btn" style={{ minHeight: 44 }}>
            로그인
          </Link>
          <Link
            href="/signup"
            className="btn btn-primary"
            style={{ minHeight: 44, fontWeight: 500 }}
          >
            회원가입
          </Link>
        </nav>
      </PublicHeader>

      <section className="container hero">
        <div className="hero-text">
          <h1>
            궁금한 건 AI가 먼저,
            <br />
            필요하면 상담원이 바로.
          </h1>
          <p>
            AI가 자주 묻는 질문에 먼저 답하고, 해결되지 않으면 상담원에게 연결해
            드립니다. 상담원을 보호하기 위해 폭언은 자동으로 감지되어 경고 및
            이용 제한이 적용됩니다.
          </p>
          <div className="stack gap-12" style={{ flexDirection: "row", flexWrap: "wrap", paddingTop: 8 }}>
            <Link href="/login" className="btn btn-primary btn-lg">
              상담 시작하기
            </Link>
            <Link href="/login" className="btn btn-lg">
              상담원 · 관리자 로그인
            </Link>
          </div>
        </div>
        <div className="placeholder">[서비스 일러스트 / 채팅 미리보기 이미지]</div>
      </section>

      <section className="container features">
        {FEATURES.map((f) => (
          <div key={f.title} className="card feature">
            <div className="icon-box">
              <Icon name={f.icon} size={22} />
            </div>
            <h2>{f.title}</h2>
            <p>{f.text}</p>
          </div>
        ))}
      </section>

      <footer className="footer">
        <div className="container">SafeDesk · AICC 채팅 상담 플랫폼 (가칭)</div>
      </footer>
    </>
  );
}
