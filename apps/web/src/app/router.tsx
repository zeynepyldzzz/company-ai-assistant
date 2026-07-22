import { createBrowserRouter } from "react-router";
import { RequireAuth } from "@/auth/auth-guard";
import { RequireRole } from "@/auth/role-guard";
import { AppLayout } from "@/layouts/app-layout";
import { LoginPage } from "@/pages/login-page";
import { DashboardPage } from "@/pages/dashboard-page";
import { AdminDashboardPage } from "@/pages/admin/admin-dashboard-page";
import { EmployeesPage } from "@/pages/directory/employees-page";
import { DepartmentsPage } from "@/pages/directory/departments-page";
import { DepartmentDetailPage } from "@/pages/directory/department-detail-page";
import { PhonebookPage } from "@/pages/directory/phonebook-page";
import { MenuPage } from "@/pages/menu/menu-page";
import { ShuttleRoutesPage } from "@/pages/shuttle/shuttle-routes-page";
import { ShuttleRouteDetailPage } from "@/pages/shuttle/shuttle-route-detail-page";
import { ShuttleRecommendationPage } from "@/pages/shuttle/shuttle-recommendation-page";

export const router = createBrowserRouter([
  { path: "/login", element: <LoginPage /> },
  {
    element: <RequireAuth />,
    children: [
      {
        element: <AppLayout />,
        children: [
          { path: "/", element: <DashboardPage /> },
          { path: "/directory/employees", element: <EmployeesPage /> },
          { path: "/directory/departments", element: <DepartmentsPage /> },
          { path: "/directory/departments/:id", element: <DepartmentDetailPage /> },
          { path: "/directory/phonebook", element: <PhonebookPage /> },
          { path: "/menu", element: <MenuPage /> },
          { path: "/shuttle/routes", element: <ShuttleRoutesPage /> },
          { path: "/shuttle/routes/:id", element: <ShuttleRouteDetailPage /> },
          { path: "/shuttle/recommendation", element: <ShuttleRecommendationPage /> },
          {
            element: <RequireRole roles={["admin"]} />,
            children: [{ path: "/admin", element: <AdminDashboardPage /> }],
          },
        ],
      },
    ],
  },
]);
