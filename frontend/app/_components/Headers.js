import Link from "next/link";
import Icon from "./Icon";

function Brand({ href, label = "SafeDesk" }) {
  return (
    <Link href={href} className="brand">
      <Icon name="shield" size={22} />
      <span>{label}</span>
    </Link>
  );
}

function Nav({ items, active }) {
  return (
    <nav className="nav">
      {items.map((item) => (
        <Link
          key={item.href}
          href={item.href}
          className={item.key === active ? "on" : undefined}
        >
          {item.label}
        </Link>
      ))}
    </nav>
  );
}

/** 로그인 · 회원가입 등에서 쓰는 단순 헤더 */
export function PublicHeader({ children }) {
  return (
    <header className="hdr">
      <div className="container hdr-inner" style={{ padding: "14px 24px" }}>
        <Brand href="/" />
        {children}
      </div>
    </header>
  );
}

const CUSTOMER_NAV = [
  { key: "home", href: "/customer", label: "홈" },
  { key: "history", href: "/customer/history", label: "내 상담 이력" },
];

export function CustomerHeader({ active }) {
  return (
    <header className="hdr">
      <div className="hdr-inner" style={{ maxWidth: 1160 }}>
        <div className="hdr-left">
          <Brand href="/customer" />
          <Nav items={CUSTOMER_NAV} active={active} />
        </div>
        <div className="hdr-right">
          <span>[고객명] 님</span>
          <Link href="/login" className="logout">
            로그아웃
          </Link>
        </div>
      </div>
    </header>
  );
}

const AGENT_NAV = [
  { key: "queue", href: "/agent", label: "상담 대기열" },
  { key: "chat", href: "/agent/chat", label: "진행 중 상담" },
  { key: "history", href: "/agent/history", label: "상담 이력" },
];

export function AgentHeader({ active, busy = false }) {
  return (
    <header className="hdr hdr-agent">
      <div className="hdr-inner">
        <div className="hdr-left">
          <Brand href="/agent" label="SafeDesk 상담원" />
          <Nav items={AGENT_NAV} active={active} />
        </div>
        <div className="hdr-right">
          <span className={busy ? "pill-status busy" : "pill-status"}>
            {busy ? "● 상담 중" : "● 상담 가능"}
          </span>
          <span>[상담원명]</span>
          <Link href="/login" className="logout">
            로그아웃
          </Link>
        </div>
      </div>
    </header>
  );
}

const ADMIN_NAV = [
  { key: "dashboard", href: "/admin", label: "대시보드" },
  { key: "sanctions", href: "/admin/sanctions", label: "제재 관리" },
  { key: "policy", href: "/admin/policy", label: "정책 관리" },
  { key: "history", href: "/admin/history", label: "상담 이력" },
];

export function AdminHeader({ active }) {
  return (
    <header className="hdr hdr-admin">
      <div className="hdr-inner">
        <div className="hdr-left">
          <Brand href="/admin" label="SafeDesk 관리자" />
          <Nav items={ADMIN_NAV} active={active} />
        </div>
        <div className="hdr-right">
          <span>[관리자명]</span>
          <Link href="/login" className="logout">
            로그아웃
          </Link>
        </div>
      </div>
    </header>
  );
}
