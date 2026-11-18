/** @type {import('next').NextConfig} */
const nextConfig = {
  /* config options here */
  cacheComponents: true,
  partialPrefetching: true,
  experimental: {
    // 고객·상담원·관리자 화면은 RoleGuard 가 로그인 확인 전에 아무것도 그리지 않아
    // 모든 페이지에 「instant navigation 검사 불가」 Insight 가 뜬다. instant 를 직접 적은 화면만 검사한다.
    instantInsights: {
      validationLevel: "manual-warning",
    },
  },
};

export default nextConfig;
