import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/auth/auth-context";
import { ApiError } from "@/api/client";
import { createVehicle, listVehicles, updateMaintenanceStatus, updateVehicle } from "@/api/vehicle";
import type { Vehicle } from "@company/shared";

const STATUS_LABELS = {
  available: "Uygun",
  maintenance: "Bakımda",
} as const;

function errorMessage(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback;
}

export function AdminVehiclesPage() {
  const { token } = useAuth();
  const queryClient = useQueryClient();
  const [editingId, setEditingId] = useState<number | null>(null);
  const [newPlate, setNewPlate] = useState("");
  const [newModel, setNewModel] = useState("");
  const [editPlate, setEditPlate] = useState("");
  const [editModel, setEditModel] = useState("");

  const {
    data: vehicles,
    isLoading,
    isError,
  } = useQuery({
    queryKey: ["admin", "vehicles"],
    queryFn: () => listVehicles(token!),
    enabled: Boolean(token),
  });

  function invalidate() {
    queryClient.invalidateQueries({ queryKey: ["admin", "vehicles"] });
  }

  const createMutation = useMutation({
    mutationFn: () => createVehicle(token!, { plate: newPlate, model: newModel || undefined }),
    onSuccess: () => {
      toast.success("Araç eklendi");
      setNewPlate("");
      setNewModel("");
      invalidate();
    },
    onError: (error: unknown) => toast.error(errorMessage(error, "Araç eklenemedi")),
  });

  const updateMutation = useMutation({
    mutationFn: (id: number) =>
      updateVehicle(token!, id, { plate: editPlate, model: editModel || undefined }),
    onSuccess: () => {
      toast.success("Araç güncellendi");
      setEditingId(null);
      invalidate();
    },
    onError: (error: unknown) => toast.error(errorMessage(error, "Araç güncellenemedi")),
  });

  const maintenanceMutation = useMutation({
    mutationFn: (vehicle: Vehicle) =>
      updateMaintenanceStatus(token!, vehicle.id, {
        maintenanceStatus: vehicle.maintenanceStatus === "available" ? "maintenance" : "available",
      }),
    onSuccess: () => {
      toast.success("Bakım durumu güncellendi");
      invalidate();
    },
    onError: (error: unknown) => toast.error(errorMessage(error, "Bakım durumu güncellenemedi")),
  });

  function startEdit(vehicle: Vehicle) {
    setEditingId(vehicle.id);
    setEditPlate(vehicle.plate);
    setEditModel(vehicle.model ?? "");
  }

  function handleCreate() {
    if (!newPlate.trim()) return;
    createMutation.mutate();
  }

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-xl font-semibold">Araç Yönetimi</h1>
        <p className="text-muted-foreground text-sm">
          Araç ekle, güncelle ve bakım durumunu işaretle.
        </p>
      </div>

      <div className="flex max-w-lg items-end gap-2 rounded-lg border p-4">
        <div className="flex-1 space-y-1.5">
          <Label htmlFor="new-plate">Plaka</Label>
          <Input
            id="new-plate"
            placeholder="34 ABC 123"
            value={newPlate}
            onChange={(event) => setNewPlate(event.target.value)}
          />
        </div>
        <div className="flex-1 space-y-1.5">
          <Label htmlFor="new-model">Model</Label>
          <Input
            id="new-model"
            placeholder="Renault Megane"
            value={newModel}
            onChange={(event) => setNewModel(event.target.value)}
          />
        </div>
        <Button
          type="button"
          disabled={!newPlate.trim() || createMutation.isPending}
          onClick={handleCreate}
        >
          {createMutation.isPending ? "Ekleniyor…" : "Araç Ekle"}
        </Button>
      </div>

      {isLoading && <p className="text-muted-foreground text-sm">Yükleniyor…</p>}
      {isError && <p className="text-destructive text-sm">Araçlar yüklenemedi.</p>}

      {vehicles && (
        <div className="overflow-x-auto rounded-lg border">
          <table className="w-full text-sm">
            <thead className="bg-muted/50">
              <tr>
                <th className="px-4 py-2 text-left font-medium">Plaka</th>
                <th className="px-4 py-2 text-left font-medium">Model</th>
                <th className="px-4 py-2 text-left font-medium">Durum</th>
                <th className="px-4 py-2 text-right font-medium">İşlemler</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {vehicles.length === 0 ? (
                <tr>
                  <td colSpan={4} className="text-muted-foreground px-4 py-3 text-center">
                    Kayıtlı araç bulunamadı.
                  </td>
                </tr>
              ) : (
                vehicles.map((vehicle) =>
                  editingId === vehicle.id ? (
                    <tr key={vehicle.id}>
                      <td className="px-4 py-2">
                        <Input
                          value={editPlate}
                          onChange={(event) => setEditPlate(event.target.value)}
                        />
                      </td>
                      <td className="px-4 py-2">
                        <Input
                          value={editModel}
                          onChange={(event) => setEditModel(event.target.value)}
                        />
                      </td>
                      <td className="text-muted-foreground px-4 py-2">
                        {STATUS_LABELS[vehicle.maintenanceStatus]}
                      </td>
                      <td className="px-4 py-2 text-right">
                        <div className="flex justify-end gap-2">
                          <Button
                            type="button"
                            size="sm"
                            disabled={!editPlate.trim() || updateMutation.isPending}
                            onClick={() => updateMutation.mutate(vehicle.id)}
                          >
                            Kaydet
                          </Button>
                          <Button
                            type="button"
                            size="sm"
                            variant="outline"
                            onClick={() => setEditingId(null)}
                          >
                            İptal
                          </Button>
                        </div>
                      </td>
                    </tr>
                  ) : (
                    <tr key={vehicle.id}>
                      <td className="px-4 py-2 font-medium">{vehicle.plate}</td>
                      <td className="text-muted-foreground px-4 py-2">{vehicle.model ?? "—"}</td>
                      <td className="px-4 py-2">{STATUS_LABELS[vehicle.maintenanceStatus]}</td>
                      <td className="px-4 py-2 text-right">
                        <div className="flex justify-end gap-2">
                          <Button
                            type="button"
                            size="sm"
                            variant="outline"
                            onClick={() => startEdit(vehicle)}
                          >
                            Düzenle
                          </Button>
                          <Button
                            type="button"
                            size="sm"
                            variant="outline"
                            disabled={maintenanceMutation.isPending}
                            onClick={() => maintenanceMutation.mutate(vehicle)}
                          >
                            {vehicle.maintenanceStatus === "available"
                              ? "Bakıma Al"
                              : "Kullanıma Aç"}
                          </Button>
                        </div>
                      </td>
                    </tr>
                  )
                )
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
