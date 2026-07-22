import { useQuery } from "@tanstack/react-query";
import { Link, useLocation, useParams } from "react-router";
import { useAuth } from "@/auth/auth-context";
import { getShuttlePlate, getShuttleStops } from "@/api/shuttle";

export function ShuttleRouteDetailPage() {
  const { id } = useParams<{ id: string }>();
  const location = useLocation();
  const { token } = useAuth();
  const routeId = Number(id);
  const routeName = (location.state as { name?: string } | null)?.name;

  const plateQuery = useQuery({
    queryKey: ["shuttle-plate", routeId],
    queryFn: () => getShuttlePlate(routeId, token!),
    enabled: Boolean(token) && Number.isFinite(routeId),
  });

  const stopsQuery = useQuery({
    queryKey: ["shuttle-stops", routeId],
    queryFn: () => getShuttleStops(routeId, token!),
    enabled: Boolean(token) && Number.isFinite(routeId),
  });

  const isLoading = plateQuery.isLoading || stopsQuery.isLoading;
  const isError = plateQuery.isError || stopsQuery.isError;

  return (
    <div className="space-y-4">
      <Link to="/shuttle/routes" className="text-primary text-sm hover:underline">
        ← Servis Güzergahları
      </Link>

      {isLoading && <p className="text-muted-foreground text-sm">Yükleniyor…</p>}
      {isError && <p className="text-destructive text-sm">Güzergah bilgisi yüklenemedi.</p>}

      {plateQuery.data && (
        <div className="space-y-1">
          <h1 className="text-xl font-semibold">{routeName ?? "Güzergah Detayı"}</h1>
          <p className="text-muted-foreground text-sm">
            Güncel plaka:{" "}
            <span className="text-foreground font-medium">
              {plateQuery.data.plateNumber ?? "Atanmamış"}
            </span>
          </p>
        </div>
      )}

      {stopsQuery.data && (
        <div className="space-y-1">
          <h2 className="text-sm font-medium">Duraklar ve Saatler</h2>
          {stopsQuery.data.length === 0 ? (
            <p className="text-muted-foreground text-sm">Bu güzergah için durak tanımlanmamış.</p>
          ) : (
            <ul className="divide-y rounded-lg border">
              {stopsQuery.data.map((stop) => (
                <li key={stop.id} className="flex items-center justify-between gap-3 px-4 py-3">
                  <span className="text-sm font-medium">
                    {stop.orderIndex}. {stop.name}
                  </span>
                  <span className="text-muted-foreground text-sm">{stop.time ?? "—"}</span>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}
