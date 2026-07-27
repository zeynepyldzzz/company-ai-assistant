import { Navigate, Outlet } from "react-router";
import type { Role, AdminSubRole } from "@company/shared";
import { useAuth } from "./auth-context";

export function RequireRole({
  roles,
  subRoles,
}: {
  roles: Role[];
  subRoles?: AdminSubRole[];
}) {
  const { user } = useAuth();

  if (!user || !roles.includes(user.role)) {
    return <Navigate to="/" replace />;
  }

  // subRoles verilmisse alt rol de eslenmeli (backend guard'ini yansitir:
  // or. A-6 knowledge-base uclari yalnizca hr_admin / system_admin).
  if (subRoles && (!user.subRole || !subRoles.includes(user.subRole))) {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
}
