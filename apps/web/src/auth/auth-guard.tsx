import { Navigate, Outlet, useLocation } from "react-router";
import { useAuth } from "./auth-context";

export function RequireAuth() {
  const { token } = useAuth();
  const location = useLocation();

  if (!token) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  return <Outlet />;
}
