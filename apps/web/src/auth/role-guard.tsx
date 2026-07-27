import { Navigate, Outlet } from "react-router";
import type { AdminSubRole, Role } from "@company/shared";
import { useAuth } from "./auth-context";

export function RequireRole({ roles }: { roles: Role[] }) {
  const { user } = useAuth();

  if (!user || !roles.includes(user.role)) {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
}

// fleet_admin gibi admin alt-rollerine ozel uclar icin (B-9): system_admin her zaman gecer,
// cunku V3 seed'inde tum modul izinlerine sahiptir (bkz. AdminVehicleController).
export function RequireSubRole({ subRoles }: { subRoles: AdminSubRole[] }) {
  const { user } = useAuth();

  if (
    !user ||
    !user.subRole ||
    !(subRoles.includes(user.subRole) || user.subRole === "system_admin")
  ) {
    return <Navigate to="/admin" replace />;
  }

  return <Outlet />;
}
