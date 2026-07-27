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
import { PhonebookPage } from "@/pages/directory/phonebook-page";
import { MenuPage } from "@/pages/menu/menu-page";
import { ShuttleRoutesPage } from "@/pages/shuttle/shuttle-routes-page";
import { ShuttleRouteDetailPage } from "@/pages/shuttle/shuttle-route-detail-page";
import { ShuttleRecommendationPage } from "@/pages/shuttle/shuttle-recommendation-page";
import { SchedulePage } from "@/pages/schedule/schedule-page";
import { VehicleReservationPage } from "@/pages/vehicle/vehicle-reservation-page";
import { AdminSchedulesPage } from "@/pages/admin/admin-schedules-page";
import { AdminVehiclesPage } from "@/pages/admin/admin-vehicles-page";
import { KnowledgeBasePage } from "@/pages/admin/knowledge-base/knowledge-base-page";
import { AdminSurveysPage } from "@/pages/admin/surveys/admin-surveys-page";
import { AdminAnnouncementsPage } from "@/pages/admin/announcements/admin-announcements-page";

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
          { path: "/directory/phonebook", element: <PhonebookPage /> },
          { path: "/menu", element: <MenuPage /> },
          { path: "/my-schedule", element: <SchedulePage /> },
          { path: "/shuttle/routes", element: <ShuttleRoutesPage /> },
          { path: "/shuttle/routes/:id", element: <ShuttleRouteDetailPage /> },
          { path: "/shuttle/recommendation", element: <ShuttleRecommendationPage /> },
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
            children: [{ path: "/admin/knowledge-base", element: <KnowledgeBasePage /> }],
          },
        ],
      },
    ],
  },
]);
