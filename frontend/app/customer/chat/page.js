import Link from "next/link";
import Icon from "../../_components/Icon";

export const metadata = { title: "AI 상담 · SafeDesk" };

function AiAvatar() {
  return <div className="avatar">AI</div>;
}

export default function AiChatPage() {
  return (
    <div className="chat-page">
      <div className="chat-shell">
        <div className="chat-top">
          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <Link href="/customer" aria-label="뒤로" className="icon-link">
              <Icon name="back" />
            </Link>
            <div className="stack">
              <span className="bold" style={{ fontSize: 16 }}>AI 상담</span>
              <span className="muted" style={{ fontSize: 12 }}>
                상태 BOT · FAQ 기반 자동 응답
              </span>
            </div>
          </div>
          <Link href="/customer/waiting" className="btn btn-outline-primary btn-sm">
            <Icon name="user" size={16} />
            <span>상담원 연결</span>
          </Link>
        </div>

        <div className="chat-body">
          <div className="center-note">YYYY.MM.DD 상담 시작</div>

          <div className="msg-row">
            <AiAvatar />
            <div className="stack gap-8">
              <div className="bubble">
                안녕하세요, 120 다산콜센터 AI 상담입니다. 궁금한 내용을 입력하시거나
                아래 자주 묻는 질문을 선택해 주세요.
              </div>
              <div className="chip-row">
                <button type="button" className="chip">[FAQ 키워드 1]</button>
                <button type="button" className="chip">[FAQ 키워드 2]</button>
                <button type="button" className="chip">[FAQ 키워드 3]</button>
              </div>
            </div>
          </div>

          <div className="bubble-me">[고객 문의 메시지]</div>

          <div className="msg-row">
            <AiAvatar />
            <div className="bubble">
              [FAQ 매칭 답변 — 한 글자씩 순차 출력]
              <span className="cursor" />
            </div>
          </div>

          <div className="bubble-me">[FAQ에 없는 문의]</div>

          <div className="msg-row">
            <AiAvatar />
            <div className="bubble stack gap-12" style={{ padding: 14 }}>
              <span>
                죄송합니다. 해당 문의에 대한 답변을 찾지 못했어요. 상담원과 직접
                상담하시겠어요?
              </span>
              <Link href="/customer/waiting" className="btn btn-primary btn-sm" style={{ alignSelf: "flex-start" }}>
                상담원 연결하기
              </Link>
            </div>
          </div>
        </div>

        <div className="chat-foot">
          <div className="chat-input">
            <label htmlFor="ai-input" className="sr-only">메시지 입력</label>
            <input id="ai-input" className="input" type="text" placeholder="메시지를 입력하세요" style={{ borderRadius: 10 }} />
            <button type="button" className="send" aria-label="전송">
              <Icon name="send" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
