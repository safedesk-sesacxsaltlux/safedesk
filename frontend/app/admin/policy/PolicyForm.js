"use client";

import { useEffect, useState } from "react";
import { getAccessToken } from "../../_lib/auth";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL;

function toForm(policy) {
  return {
    warningThreshold: String(policy.warningThreshold),
    autoCloseThreshold: String(policy.autoCloseThreshold),
    first: String(policy.suspensionDays.first),
    second: String(policy.suspensionDays.second),
    thirdOrMore: String(policy.suspensionDays.thirdOrMore),
  };
}

function toNumber(value) {
  return value === "" ? null : Number(value);
}

function toRequest(form) {
  return {
    warningThreshold: toNumber(form.warningThreshold),
    autoCloseThreshold: toNumber(form.autoCloseThreshold),
    suspensionDays: {
      first: toNumber(form.first),
      second: toNumber(form.second),
      thirdOrMore: toNumber(form.thirdOrMore),
    },
  };
}

export default function PolicyForm() {
  const [status, setStatus] = useState("loading");
  const [form, setForm] = useState(null);
  const [defaults, setDefaults] = useState(null);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      try {
        const res = await fetch(`${API_BASE_URL}/api/admin/policies`, {
          headers: { Authorization: `Bearer ${getAccessToken()}` },
        });
        const body = await res.json();
        if (cancelled) return;
        if (!res.ok || !body.success) {
          setStatus("error");
          return;
        }
        setForm(toForm(body.data.current));
        setDefaults(toForm(body.data.defaults));
        setStatus("ready");
      } catch {
        if (!cancelled) setStatus("error");
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, []);

  function change(e) {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
    setMessage(null);
  }

  function resetToDefaults() {
    setForm(defaults);
    setMessage(null);
  }

  async function save() {
    setSaving(true);
    setMessage(null);
    try {
      const res = await fetch(`${API_BASE_URL}/api/admin/policies`, {
        method: "PUT",
        headers: {
          Authorization: `Bearer ${getAccessToken()}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify(toRequest(form)),
      });
      const body = await res.json();
      if (!res.ok || !body.success) {
        setMessage({ type: "error", text: body.message });
        return;
      }
      setForm(toForm(body.data));
      setMessage({ type: "ok", text: body.message });
    } catch {
      setMessage({ type: "error", text: "저장하지 못했습니다. 잠시 후 다시 시도해주세요." });
    } finally {
      setSaving(false);
    }
  }

  if (status === "loading") {
    return <p className="muted">정책을 불러오는 중입니다.</p>;
  }
  if (status === "error") {
    return <p className="msg-err">정책을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.</p>;
  }

  return (
    <>
      <section className="card stack gap-16">
        <h2>상담 중 처리 기준</h2>
        <div className="policy-grid">
          <div className="field" style={{ flex: "1 1 260px" }}>
            <label htmlFor="pl-warn">경고 발송 기준</label>
            <div className="inline-num">
              <span>폭언</span>
              <input id="pl-warn" name="warningThreshold" className="input sm" type="number"
                value={form.warningThreshold} onChange={change} min={1} max={5} />
              <span>회째 감지 시 경고</span>
            </div>
          </div>
          <div className="field" style={{ flex: "1 1 260px" }}>
            <label htmlFor="pl-end">자동 종료 기준</label>
            <div className="inline-num">
              <span>같은 상담 중</span>
              <input id="pl-end" name="autoCloseThreshold" className="input sm" type="number"
                value={form.autoCloseThreshold} onChange={change} min={1} max={5} />
              <span>회 감지 시 종료</span>
            </div>
            <span className="small muted">경고 기준보다 커야 합니다.</span>
          </div>
        </div>
      </section>

      <section className="card stack gap-16">
        <div className="stack gap-4">
          <h2>누적 위반별 이용정지</h2>
          <span className="small muted">강제 종료 1회 = 위반 1회 누적</span>
        </div>

        <div className="inline-num" style={{ borderBottom: "1px solid var(--line-soft)", paddingBottom: 14 }}>
          <span className="bold" style={{ width: 110 }}>누적 1회</span>
          <label htmlFor="pl-d1" className="muted small">정지 기간</label>
          <input id="pl-d1" name="first" className="input sm" type="number"
            value={form.first} onChange={change} min={1} max={365} />
          <span>일</span>
        </div>
        <div className="inline-num" style={{ borderBottom: "1px solid var(--line-soft)", paddingBottom: 14 }}>
          <span className="bold" style={{ width: 110 }}>누적 2회</span>
          <label htmlFor="pl-d2" className="muted small">정지 기간</label>
          <input id="pl-d2" name="second" className="input sm" type="number"
            value={form.second} onChange={change} min={1} max={365} />
          <span>일</span>
        </div>
        <div className="inline-num">
          <span className="bold" style={{ width: 110 }}>누적 3회 이상</span>
          <label htmlFor="pl-d3" className="muted small">정지 기간</label>
          <input id="pl-d3" name="thirdOrMore" className="input sm" type="number"
            value={form.thirdOrMore} onChange={change} min={1} max={365} />
          <span>일</span>
        </div>
      </section>

      <div style={{ display: "flex", gap: 10, justifyContent: "flex-end", alignItems: "center", flexWrap: "wrap" }}>
        {message && (
          <span className={message.type === "error" ? "msg-err" : "small muted"} role="status">
            {message.text}
          </span>
        )}
        <button type="button" className="btn" onClick={resetToDefaults} disabled={saving}>기본값으로 되돌리기</button>
        <button type="button" className="btn btn-primary" onClick={save} disabled={saving}
          style={{ background: "var(--admin)", borderColor: "var(--admin)" }}>
          {saving ? "저장 중" : "저장"}
        </button>
      </div>
    </>
  );
}
