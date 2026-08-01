import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/auth/auth-context";
import { ApiError } from "@/api/client";
import { deleteShuttleRoute, listShuttleRoutes } from "@/api/shuttle";
import { ShuttleRouteFormSheet } from "./shuttle-route-form-sheet";

const ALL_ROUTES_PAGE_SIZE = 100;

// B-17: admin servis guzergahi CRUD ekrani (FR-73, shuttle_admin/system_admin).
export function AdminShuttleRoutesPage() {
  const { token } = useAuth();
  const queryClient = useQueryClient();

  const { data: routesPage, isLoading, isError } = useQuery({
    queryKey: ["shuttle-routes", "admin-list"],
    queryFn: () => listShuttleRoutes({ page: 0, pageSize: ALL_ROUTES_PAGE_SIZE }, token!),
    enabled: Boolean(token),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deleteShuttleRoute(id, token!),
    onSuccess: () => {
      toast.success("Güzergah silindi.");
      queryClient.invalidateQueries({ queryKey: ["shuttle-routes"] });
    },
    onError: (error) => {
      const message = error instanceof ApiError ? error.message : "Güzergah silinemedi.";
      toast.error(message);
    },
  });

  const routes = routesPage?.data ?? [];

  return (
    <div className="space-y-4">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-xl font-semibold">Servis Güzergahı Yönetimi</h1>
          <p className="text-muted-foreground text-sm">
            Güzergah oluşturun, durakları/şoförü düzenleyin, silin.
          </p>
        </div>
        <ShuttleRouteFormSheet />
      </div>

      {isLoading && <p className="text-muted-foreground text-sm">Yükleniyor…</p>}
      {isError && <p className="text-destructive text-sm">Güzergahlar yüklenemedi.</p>}

      {routesPage && (
        <div className="overflow-x-auto rounded-lg border">
          <table className="w-full text-sm">
            <thead className="bg-muted/50">
              <tr>
                <th className="px-4 py-2 text-left font-medium">Ad</th>
                <th className="px-4 py-2 text-left font-medium">Plaka</th>
                <th className="px-4 py-2 text-left font-medium">Şoför</th>
                <th className="px-4 py-2 text-right font-medium">İşlemler</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {routes.length === 0 ? (
                <tr>
                  <td colSpan={4} className="text-muted-foreground px-4 py-3 text-center">
                    Henüz güzergah yok.
                  </td>
                </tr>
              ) : (
                routes.map((route) => (
                  <tr key={route.id}>
                    <td className="px-4 py-2 font-medium">{route.name}</td>
                    <td className="text-muted-foreground px-4 py-2">
                      {route.plateNumber ?? "Atanmamış"}
                    </td>
                    <td className="text-muted-foreground px-4 py-2">
                      {route.driverName ?? "Atanmamış"}
                    </td>
                    <td className="px-4 py-2">
                      <div className="flex justify-end gap-2">
                        <ShuttleRouteFormSheet route={route} />
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => {
                            if (confirm(`${route.name} silinsin mi?`)) {
                              deleteMutation.mutate(route.id);
                            }
                          }}
                          disabled={deleteMutation.isPending}
                        >
                          <Trash2 />
                          Sil
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
