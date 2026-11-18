"use client";

// 목록 아래 이전·다음 페이지 버튼 (PageResponse: page, totalPages, hasNext)
export default function Pager({ page, totalPages, hasNext, onChange }) {
  if (!totalPages || totalPages <= 1) return null;
  return (
    <div className="pager" style={{ alignItems: "center" }}>
      <button type="button" onClick={() => onChange(page - 1)} disabled={page === 0}>이전</button>
      <span className="small muted">{page + 1} / {totalPages}</span>
      <button type="button" onClick={() => onChange(page + 1)} disabled={!hasNext}>다음</button>
    </div>
  );
}
