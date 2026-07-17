import { createBrowserRouter } from "react-router";
import { RequireAuth } from "@/auth/auth-guard";
import { RequireRole } from "@/auth/role-guard";
import { AppLayout } from "@/layouts/app-layout";
import { LoginPage } from "@/pages/login-page";
import { DashboardPage } from "@/pages/dashboard-page";
import { AdminDashboardPage } from "@/pages/admin/admin-dashboard-page";

export const router = createBrowserRouter([
  { path: "/login", element: <LoginPage /> },
  {
    element: <RequireAuth />,
    children: [
      {
        element: <AppLayout />,
        children: [
          { path: "/", element: <DashboardPage /> },
          {
            element: <RequireRole roles={["admin"]} />,
            children: [{ path: "/admin", element: <AdminDashboardPage /> }],
          },
        ],
      },
    ],
  },
]);
