import { Navigate, Outlet } from "react-router";
import type { Role } from "@company/shared";
import { useAuth } from "./auth-context";

export function RequireRole({ roles }: { roles: Role[] }) {
  const { user } = useAuth();

  if (!user || !roles.includes(user.role)) {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
}
