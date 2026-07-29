import { Link } from "react-router";
import { useQuery } from "@tanstack/react-query";
import {
  CalendarDays,
  ClipboardList,
  Car,
  BookText,
  Users,
  KeyRound,
  ChefHat,
  Building2,
} from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { useAuth } from "@/auth/auth-context";
import { searchEmployees } from "@/api/directory";
import { listActiveSurveys } from "@/api/survey";
import { listVehicles } from "@/api/vehicle";
import { listDocuments } from "@/api/knowledge-base";

function StatTile({
  label,
  value,
  isLoading,
  isError,
}: {
  label: string;
  value: number | undefined;
  isLoading: boolean;
  isError: boolean;
}) {
  return (
    <Card>
      <CardContent className="space-y-1">
        <p className="text-muted-foreground text-[11px] font-bold tracking-[0.04em] uppercase">
          {label}
        </p>
        <p className="text-2xl font-extrabold">
          {isLoading ? "…" : isError ? "—" : (value ?? 0)}
        </p>
      </CardContent>
    </Card>
  );
}

const links = [
  { to: "/admin/schedules", label: "Çalışan Düzeni", icon: CalendarDays },
  { to: "/admin/surveys", label: "Anketler", icon: ClipboardList },
  { to: "/admin/knowledge-base", label: "Dokümanlar", icon: BookText },
  { to: "/admin/vehicles", label: "Araç Yönetimi", icon: Car },
  { to: "/admin/employees", label: "Çalışan Yönetimi", icon: Users },
  { to: "/admin/roles", label: "Rol / İzin Yönetimi", icon: KeyRound },
  { to: "/admin/menu", label: "Menü Yönetimi", icon: ChefHat },
  { to: "/admin/departments", label: "Departman Yönetimi", icon: Building2 },
];

export function AdminDashboardPage() {
  const { token } = useAuth();

  const employeesQuery = useQuery({
    queryKey: ["employees", "count"],
    queryFn: () => searchEmployees({ page: 0, pageSize: 1 }, token!),
    enabled: Boolean(token),
  });

  const surveysQuery = useQuery({
    queryKey: ["surveys", "active"],
    queryFn: () => listActiveSurveys(token!),
    enabled: Boolean(token),
  });

  const vehiclesQuery = useQuery({
    queryKey: ["vehicles", "all"],
    queryFn: () => listVehicles(token!),
    enabled: Boolean(token),
  });

  const documentsQuery = useQuery({
    queryKey: ["documents", "count"],
    queryFn: () => listDocuments({ page: 0, pageSize: 1 }, token!),
    enabled: Boolean(token),
  });

  return (
    <div className="space-y-8">
      <h1 className="text-[22px] font-extrabold">Yönetim Paneli</h1>

      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        <StatTile
          label="Toplam Çalışan"
          value={employeesQuery.data?.total}
          isLoading={employeesQuery.isLoading}
          isError={employeesQuery.isError}
        />
        <StatTile
          label="Aktif Anket"
          value={surveysQuery.data?.length}
          isLoading={surveysQuery.isLoading}
          isError={surveysQuery.isError}
        />
        <StatTile
          label="Araç Filosu"
          value={vehiclesQuery.data?.length}
          isLoading={vehiclesQuery.isLoading}
          isError={vehiclesQuery.isError}
        />
        <StatTile
          label="Doküman"
          value={documentsQuery.data?.total}
          isLoading={documentsQuery.isLoading}
          isError={documentsQuery.isError}
        />
      </div>

      <section className="space-y-3">
        <p className="text-muted-foreground text-[11px] font-bold tracking-[0.04em] uppercase">
          Yönetim Ekranları
        </p>
        <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
          {links.map(({ to, label, icon: Icon }) => (
            <Link key={to} to={to}>
              <Card className="hover:border-primary/40 transition-colors">
                <CardContent className="flex flex-col items-center gap-2 py-5 text-center">
                  <Icon className="text-primary size-5" />
                  <span className="text-sm font-medium">{label}</span>
                </CardContent>
              </Card>
            </Link>
          ))}
        </div>
      </section>
    </div>
  );
}
