import Link from "next/link";

// 표 행 끝의 상세 이동 버튼 (> 모양)
export default function RowLink({ href, label }) {
  return (
    <Link href={href} className="row-link" aria-label={label}>
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
           strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M9 6l6 6-6 6" />
      </svg>
    </Link>
  );
}
