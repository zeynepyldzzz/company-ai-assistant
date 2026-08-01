import { createBrowserRouter } from "react-router";
import { RequireAuth } from "@/auth/auth-guard";
import { RequireRole } from "@/auth/role-guard";
import { AppLayout } from "@/layouts/app-layout";
import { LoginPage } from "@/pages/login-page";
import { DashboardPage } from "@/pages/dashboard-page";
import { ChatPage } from "@/pages/chat/chat-page";
import { AdminDashboardPage } from "@/pages/admin/admin-dashboard-page";
import { EmployeesPage } from "@/pages/directory/employees-page";
import { DepartmentsPage } from "@/pages/directory/departments-page";
import { DepartmentDetailPage } from "@/pages/directory/department-detail-page";
import { MenuPage } from "@/pages/menu/menu-page";
import { ShuttlePage } from "@/pages/shuttle/shuttle-page";
import { SchedulePage } from "@/pages/schedule/schedule-page";
import { VehicleReservationPage } from "@/pages/vehicle/vehicle-reservation-page";
import { AdminSchedulesPage } from "@/pages/admin/admin-schedules-page";
import { AdminVehiclesPage } from "@/pages/admin/admin-vehicles-page";
import { KnowledgeBasePage } from "@/pages/admin/knowledge-base/knowledge-base-page";
import { AdminSurveysPage } from "@/pages/admin/surveys/admin-surveys-page";
import { AdminAnnouncementsPage } from "@/pages/admin/announcements/admin-announcements-page";
import { AdminEmployeesPage } from "@/pages/admin/employees/admin-employees-page";
import { AdminDepartmentsPage } from "@/pages/admin/departments/admin-departments-page";
import { AdminRolesPage } from "@/pages/admin/roles/admin-roles-page";
import { AdminReportsPage } from "@/pages/admin/reports/admin-reports-page";
import { AdminMenuPage } from "@/pages/admin/menu/admin-menu-page";

export const router = createBrowserRouter([
  { path: "/login", element: <LoginPage /> },
  {
    element: <RequireAuth />,
    children: [
      {
        element: <AppLayout />,
        children: [
          { path: "/", element: <DashboardPage /> },
          { path: "/chat", element: <ChatPage /> },
          { path: "/directory/employees", element: <EmployeesPage /> },
          { path: "/directory/departments", element: <DepartmentsPage /> },
          { path: "/directory/departments/:id", element: <DepartmentDetailPage /> },
          { path: "/menu", element: <MenuPage /> },
          { path: "/my-schedule", element: <SchedulePage /> },
          { path: "/shuttle", element: <ShuttlePage /> },
          { path: "/vehicles", element: <VehicleReservationPage /> },
          {
            element: <RequireRole roles={["admin"]} />,
            children: [
              { path: "/admin", element: <AdminDashboardPage /> },
              { path: "/admin/schedules", element: <AdminSchedulesPage /> },
              { path: "/admin/surveys", element: <AdminSurveysPage /> },
              { path: "/admin/announcements", element: <AdminAnnouncementsPage /> },
            ],
          },
          {
            // B-9: arac yonetimi yalnizca fleet_admin / system_admin (B-8 backend guard'ini yansitir)
            element: <RequireRole roles={["admin"]} subRoles={["fleet_admin", "system_admin"]} />,
            children: [{ path: "/admin/vehicles", element: <AdminVehiclesPage /> }],
          },
          {
            // A-7: bilgi tabani yalnizca hr_admin / system_admin (A-6 backend guard'ini yansitir)
            element: <RequireRole roles={["admin"]} subRoles={["hr_admin", "system_admin"]} />,
            children: [
              { path: "/admin/knowledge-base", element: <KnowledgeBasePage /> },
              // #84 (Hafta 4): calisan/departman CRUD yalnizca hr_admin / system_admin (FR-68-71).
              { path: "/admin/employees", element: <AdminEmployeesPage /> },
              { path: "/admin/departments", element: <AdminDepartmentsPage /> },
            ],
          },
          {
            // C-11 (#85): rol/izin yonetimi + rapor yalnizca system_admin (FR-80-82).
            element: <RequireRole roles={["admin"]} subRoles={["system_admin"]} />,
            children: [
              { path: "/admin/roles", element: <AdminRolesPage /> },
              { path: "/admin/reports", element: <AdminReportsPage /> },
            ],
          },
          {
            // C-2 (#18): menu yonetimi backend'de sadece hasRole('ADMIN') istiyor,
            // subRole kisitlamasi yok - herhangi bir admin excel yukleyebilir/silebilir.
            element: <RequireRole roles={["admin"]} />,
            children: [{ path: "/admin/menu", element: <AdminMenuPage /> }],
          },
        ],
      },
    ],
  },
]);
