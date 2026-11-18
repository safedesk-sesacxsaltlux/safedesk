"use client";

import { useEffect, useState } from "react";
import { getAccessToken } from "../../_lib/auth";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL;
const PAGE_SIZE = 20;

function formatDateTime(value) {
  return value.slice(0, 16).replace("T", " ").replaceAll("-", ".");
}

function formatChange(change) {
  const unit = change.field.startsWith("suspensionDays") ? "일" : "회";
  return `${change.fieldLabel} ${change.beforeValue}${unit} → ${change.afterValue}${unit}`;
}

function describe(history) {
  if (history.changes.length === 0) return "기본 정책 등록";
  return history.changes.map(formatChange).join(", ");
}

export default function PolicyHistory() {
  const [status, setStatus] = useState("loading");
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      try {
        const res = await fetch(
          `${API_BASE_URL}/api/admin/policies/histories?page=${page}&size=${PAGE_SIZE}`,
          { headers: { Authorization: `Bearer ${getAccessToken()}` } },
        );
        const body = await res.json();
        if (cancelled) return;
        if (!res.ok || !body.success) {
          setStatus("error");
          return;
        }
        setData(body.data);
        setStatus("ready");
      } catch {
        if (!cancelled) setStatus("error");
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, [page]);

  return (
    <section className="card stack gap-12">
      <h2>변경 이력</h2>

      {status === "loading" && <span className="small muted">변경 이력을 불러오는 중입니다.</span>}
      {status === "error" && <span className="msg-err">변경 이력을 불러오지 못했습니다.</span>}

      {status === "ready" && data.content.length === 0 && (
        <span className="small muted">변경 이력이 없습니다.</span>
      )}

      {status === "ready" && data.content.map((history) => (
        <span key={history.policyVersion} className="small muted">
          {formatDateTime(history.changedAt)} · {history.changedBy?.name ?? "초기 데이터"} · {describe(history)}
        </span>
      ))}

      {status === "ready" && data.totalPages > 1 && (
        <div style={{ display: "flex", gap: 8, justifyContent: "center", alignItems: "center" }}>
          <button type="button" className="btn" onClick={() => setPage(page - 1)} disabled={page === 0}>
            이전
          </button>
          <span className="small muted">{page + 1} / {data.totalPages}</span>
          <button type="button" className="btn" onClick={() => setPage(page + 1)}
            disabled={page + 1 >= data.totalPages}>
            다음
          </button>
        </div>
      )}
    </section>
  );
}
