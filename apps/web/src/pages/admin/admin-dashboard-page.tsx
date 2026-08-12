import { Link } from "react-router";
import { useQuery } from "@tanstack/react-query";
import { Card, CardContent } from "@/components/ui/card";
import { useAuth } from "@/auth/auth-context";
import { screensFor } from "@/lib/admin-screens";
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

export function AdminDashboardPage() {
  const { token, user } = useAuth();

  // A-36 (#200): ekranlar alt role gore filtreleniyor. Onceden dokuz kartin hepsi her
  // admin'e gosteriliyordu; yetki backend'de dogru calisiyordu ama kullanici bunu ancak
  // tikladiktan sonra ogreniyor, hangisinin acik oldugunu deneme yanilmayla buluyordu.
  const screens = screensFor(user?.subRole);

  // Dort sayi kartindan yalnizca "Dokuman" kisitli bir uca gidiyor
  // (/admin/knowledge-base/documents -> hr_admin + system_admin). Digerleri calisana da
  // acik uclar. Yetkisi olmayana bu sorguyu attirmak gereksiz 403 uretiyor ve kart "—"
  // gosterdigi icin panel bozukmus gibi duruyordu.
  const canSeeDocuments =
    user?.subRole === "hr_admin" || user?.subRole === "system_admin";

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
    enabled: Boolean(token) && canSeeDocuments,
  });

  return (
    <div className="space-y-8">
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
        {canSeeDocuments && (
          <StatTile
            label="Doküman"
            value={documentsQuery.data?.total}
            isLoading={documentsQuery.isLoading}
            isError={documentsQuery.isError}
          />
        )}
      </div>

      <section className="space-y-3">
        <p className="text-muted-foreground text-[11px] font-bold tracking-[0.04em] uppercase">
          Yönetim Ekranları
        </p>
        {screens.length === 0 ? (
          // Bos liste yerine acik mesaj: kullanici panelin bozuk oldugunu dusunmesin.
          // Bugun her alt rolun en az bir ekrani var, ama yeni bir alt rol eklendiginde
          // haritaya yazilmasi unutulursa burasi sessizce bos kalirdi.
          <p className="text-muted-foreground text-sm">
            Rolünüze tanımlı bir yönetim ekranı bulunmuyor.
          </p>
        ) : (
          <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
            {screens.map(({ to, label, icon: Icon }) => (
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
        )}
      </section>
    </div>
  );
}
