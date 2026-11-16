import RoleGuard from "../_components/RoleGuard";

export default function CustomerLayout({ children }) {
  return <RoleGuard role="CUSTOMER">{children}</RoleGuard>;
}
