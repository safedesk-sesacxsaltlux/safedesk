import RoleGuard from "../_components/RoleGuard";

export default function AdminLayout({ children }) {
  return <RoleGuard role="ADMIN">{children}</RoleGuard>;
}
