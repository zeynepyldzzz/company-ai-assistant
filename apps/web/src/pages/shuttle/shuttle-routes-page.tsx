import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link, useNavigate } from "react-router";
import { PaginationControls } from "@/components/pagination-controls";
import { useAuth } from "@/auth/auth-context";
import { listShuttleRoutes } from "@/api/shuttle";

const PAGE_SIZE = 10;

export function ShuttleRoutesPage() {
  const { token } = useAuth();
  const navigate = useNavigate();
  const [page, setPage] = useState(0);

  const { data, isLoading, isError } = useQuery({
    queryKey: ["shuttle-routes", page],
    queryFn: () => listShuttleRoutes({ page, pageSize: PAGE_SIZE }, token!),
    enabled: Boolean(token),
  });

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">Servis Güzergahları</h1>
        <Link to="/shuttle/recommendation" className="text-primary text-sm hover:underline">
          Bana en yakın güzergahı bul →
        </Link>
      </div>

      {isLoading && <p className="text-muted-foreground text-sm">Yükleniyor…</p>}
      {isError && <p className="text-destructive text-sm">Servis güzergahları yüklenemedi.</p>}

      {data && (
        <>
          {data.data.length === 0 ? (
            <p className="text-muted-foreground text-sm">Sonuç bulunamadı.</p>
          ) : (
            <ul className="divide-y rounded-lg border">
              {data.data.map((route) => (
                <li key={route.id}>
                  <button
                    type="button"
                    onClick={() =>
                      navigate(`/shuttle/routes/${route.id}`, { state: { name: route.name } })
                    }
                    className="hover:bg-muted flex w-full items-center justify-between gap-3 px-4 py-3 text-left transition-colors"
                  >
                    <span className="text-sm font-medium">{route.name}</span>
                    <span className="text-muted-foreground text-sm">
                      {route.plateNumber ?? "Plaka atanmamış"}
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          )}
          <PaginationControls page={data.page} pageSize={data.pageSize} total={data.total} onPageChange={setPage} />
        </>
      )}
    </div>
  );
}
