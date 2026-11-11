import { Noto_Sans_KR } from "next/font/google";
import "./globals.css";

const notoSansKr = Noto_Sans_KR({
  variable: "--font-noto",
  weight: ["400", "500", "700"],
  subsets: ["latin"],
});

export const metadata = {
  title: "SafeDesk",
  description: "AI 1차 응대와 상담원 보호 정책을 갖춘 채팅 상담 플랫폼",
};

export default function RootLayout({ children }) {
  return (
    <html lang="ko" className={notoSansKr.variable}>
      <body>{children}</body>
    </html>
  );
}
