const PATHS = {
  shield: ["M12 3l8 3v6c0 4.5-3.4 8.3-8 9-4.6-.7-8-4.5-8-9V6l8-3z"],
  shieldCheck: ["M12 3l8 3v6c0 4.5-3.4 8.3-8 9-4.6-.7-8-4.5-8-9V6l8-3z", "M9 12l2 2 4-4"],
  back: ["M15 6l-6 6 6 6"],
  user: ["M4 21c1.5-4 4.5-6 8-6s6.5 2 8 6"],
  chat: ["M21 12a8 8 0 0 1-11.6 7.1L4 20l1-4.6A8 8 0 1 1 21 12z"],
  send: ["M4 12l16-8-6 16-2-7-8-1z"],
  warn: ["M12 3l10 18H2L12 3z", "M12 10v5M12 18h.01"],
  alert: ["M12 8v5M12 16h.01"],
  lines: ["M4 12h16M4 6h16M4 18h10"],
  lock: ["M8 11V8a4 4 0 0 1 8 0v3"],
  up: ["M6 15l6-6 6 6"],
  down: ["M6 9l6 6 6-6"],
  ban: ["M6 6l12 12"],
};

export default function Icon({ name, size = 20 }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      aria-hidden="true"
    >
      {name === "user" && <circle cx="12" cy="8" r="4" />}
      {name === "alert" && <circle cx="12" cy="12" r="9" />}
      {name === "ban" && <circle cx="12" cy="12" r="9" />}
      {name === "lock" && <rect x="5" y="11" width="14" height="10" rx="2" />}
      {name === "bot" && (
        <>
          <rect x="4" y="7" width="16" height="12" rx="3" />
          <path d="M12 3v4M9 13h.01M15 13h.01" />
        </>
      )}
      {(PATHS[name] || []).map((d) => (
        <path key={d} d={d} />
      ))}
    </svg>
  );
}
