import { useQuery } from "@tanstack/react-query";
import { Link, useParams } from "react-router";
import { useAuth } from "@/auth/auth-context";
import { getDepartmentById } from "@/api/directory";

export function DepartmentDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { token } = useAuth();
  const departmentId = Number(id);

  const { data, isLoading, isError } = useQuery({
    queryKey: ["department", departmentId],
    queryFn: () => getDepartmentById(departmentId, token!),
    enabled: Boolean(token) && Number.isFinite(departmentId),
  });

  return (
    <div className="space-y-4">
      <Link to="/directory/departments" className="text-primary text-sm hover:underline">
        ← Departmanlar
      </Link>

      {isLoading && <p className="text-muted-foreground text-sm">Yükleniyor…</p>}
      {isError && <p className="text-destructive text-sm">Departman yüklenemedi.</p>}

      {data && (
        <div className="space-y-4">
          <h1 className="text-xl font-semibold">{data.name}</h1>

          <div className="space-y-1">
            <h2 className="text-sm font-medium">Sorumluluklar</h2>
            <p className="text-muted-foreground text-sm">
              {data.responsibilities ?? "Belirtilmemiş"}
            </p>
          </div>

          <div className="space-y-1">
            <h2 className="text-sm font-medium">Yönetici</h2>
            {data.managerName ? (
              <div className="text-muted-foreground text-sm">
                <p>{data.managerName}</p>
                {data.managerEmail && <p>{data.managerEmail}</p>}
                {data.managerPhone && <p>{data.managerPhone}</p>}
              </div>
            ) : (
              <p className="text-muted-foreground text-sm">Atanmamış</p>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
