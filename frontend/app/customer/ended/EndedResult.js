"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import Icon from "../../_components/Icon";
import { getChat, getMySanction } from "../_lib/chatApi";

// 서버 시간 "2026-11-25T10:00:00" (KST, 타임존 표기 없음) → "2026.11.25 10:00"
function formatDateTime(value) {
  if (!value) return "-";
  const [date, time = ""] = value.split("T");
  return `${date.replaceAll("-", ".")} ${time.slice(0, 5)}`;
}

// SCR-008 상담 종료 결과 (API-003 상담 상세)
// 정상 종료(CLOSED)와 폭언 자동 종료(TERMINATED)를 나눠 보여준다
export default function EndedResult() {
  const router = useRouter();
  const chatId = useSearchParams().get("chatId");
  const [result, setResult] = useState(null);
  // 폭언 종료일 때 적용된 이용정지 (HIS-02 currentSanction). 이미 풀렸으면 null
  const [sanction, setSanction] = useState(null);

  useEffect(() => {
    if (!chatId) {
      router.replace("/customer");
      return;
    }

    async function load() {
      const { status, body } = await getChat(chatId);
      if (status === 401) {
        router.replace("/login");
        return;
      }
      if (!body?.success) {
        router.replace("/customer");
        return;
      }
      // 정지 일수·해제 일시는 상담 상세에 없어서 내 이용 상태에서 가져온다
      if (body.data.consultationStatus === "TERMINATED") {
        const mine = await getMySanction().catch(() => null);
        setSanction(mine?.body?.data?.currentSanction ?? null);
      }
      setResult(body.data);
    }

    load().catch(() => router.replace("/customer"));
  }, [chatId, router]);

  // 불러오는 중이거나, 숨겨 두었던 이전 상담 결과가 남아 있으면 그리지 않는다 (Cache Components)
  if (!result || result.consultationId !== Number(chatId)) return null;

  if (result.consultationStatus === "TERMINATED") {
    return (
      <section className="card stack gap-20" style={{ flex: "1 1 460px", padding: 36, borderRadius: 16 }}>
        <span className="badge badge-danger bold" style={{ alignSelf: "flex-start", padding: "4px 12px", fontSize: 13 }}>
          자동 종료
        </span>
        <div
          style={{
            width: 64, height: 64, borderRadius: "50%", background: "var(--danger-soft)",
            color: "var(--danger)", display: "flex", alignItems: "center", justifyContent: "center",
          }}
        >
          <Icon name="ban" size={30} />
        </div>
        <div className="stack gap-8">
          <h1 style={{ fontSize: 26 }}>상담이 자동 종료되었습니다</h1>
          <p className="muted" style={{ fontSize: 15, lineHeight: 1.7 }}>
            같은 상담에서 부적절한 표현이 반복 감지되어 상담원 보호 정책에 따라
            상담이 종료되었습니다.
          </p>
        </div>

        <div style={{ border: "1px solid var(--line)", borderRadius: 12, overflow: "hidden" }}>
          {[
            ["종료 일시", formatDateTime(result.closedAt)],
            ["종료 사유", `반복 폭언 (경고 ${result.warningCount}회)`],
            ["누적 위반", `${result.customerViolationCount}회`],
          ].map(([k, v]) => (
            <div key={k} className="kv" style={{ padding: "14px 18px", fontSize: 15 }}>
              <span>{k}</span>
              <span style={k === "누적 위반" ? { fontWeight: 700 } : undefined}>{v}</span>
            </div>
          ))}
          {/* 정지가 이미 풀렸으면 기간 대신 안내 문구 */}
          <div className="kv" style={{ padding: "14px 18px", fontSize: 15, background: "var(--danger-soft)" }}>
            <span style={{ color: "var(--danger-text)" }}>적용 제재</span>
            <span className="bold" style={{ color: "var(--danger)" }}>
              {sanction
                ? `${sanction.suspendDays}일 이용정지 (~ ${formatDateTime(sanction.endAt).slice(0, 10)})`
                : "정책에 따른 이용정지 (현재 해제됨)"}
            </span>
          </div>
        </div>

        <Link href="/customer" className="btn btn-primary" style={{ alignSelf: "flex-start", fontWeight: 500 }}>
          홈으로
        </Link>
      </section>
    );
  }

  return (
    <section className="card stack gap-12" style={{ flex: "1 1 460px", padding: 36, borderRadius: 16 }}>
      <span className="badge badge-ok bold" style={{ alignSelf: "flex-start", padding: "4px 12px", fontSize: 13 }}>
        상담 종료
      </span>
      <h1 style={{ fontSize: 26 }}>상담이 종료되었습니다</h1>
      <p className="muted" style={{ fontSize: 15, lineHeight: 1.6 }}>이용해 주셔서 감사합니다.</p>
      <div style={{ border: "1px solid var(--line)", borderRadius: 12, overflow: "hidden" }}>
        <div className="kv" style={{ padding: "14px 18px", fontSize: 15 }}>
          <span>종료 일시</span>
          <span>{formatDateTime(result.closedAt)}</span>
        </div>
        {result.agentName && (
          <div className="kv" style={{ padding: "14px 18px", fontSize: 15 }}>
            <span>상담원</span>
            <span>{result.agentName}</span>
          </div>
        )}
      </div>
      <div className="transcript">
        <span className="bold">상담 요약</span>
        <span className="muted" style={{ fontSize: 14, whiteSpace: "pre-wrap" }}>
          {result.inquirySummary ?? "요약 없음"}
        </span>
      </div>
      <Link href="/customer" className="btn btn-primary" style={{ alignSelf: "flex-start", fontWeight: 500 }}>
        홈으로
      </Link>
    </section>
  );
}
