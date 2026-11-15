import RoleGuard from "../_components/RoleGuard";

export default function AgentLayout({ children }) {
  return <RoleGuard role="AGENT">{children}</RoleGuard>;
}
