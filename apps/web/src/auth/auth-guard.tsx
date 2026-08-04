import { Navigate, Outlet, useLocation } from "react-router";
import { useAuth } from "./auth-context";

export function RequireAuth() {
  const { token, mustChangePassword } = useAuth();
  const location = useLocation();

  if (!token) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  // A-29: sifre sistem tarafindan uretilmis gecici bir sifreyse kullanici once kendi
  // sifresini belirlemeli. Yonlendirme burada, cunku her korumali sayfa bu guard'dan
  // geciyor — tek tek sayfalara kontrol eklemek unutulmaya acik olurdu.
  if (mustChangePassword && location.pathname !== "/change-password") {
    return <Navigate to="/change-password" replace />;
  }

  return <Outlet />;
}
