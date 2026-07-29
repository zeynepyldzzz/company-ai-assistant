import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { useAuth } from "@/auth/auth-context";
import { ApiError } from "@/api/client";
import { createVehicle, listVehicles, updateMaintenanceStatus, updateVehicle } from "@/api/vehicle";
import type { Vehicle } from "@company/shared";
import { cn } from "@/lib/utils";

const STATUS_LABELS = {
  available: "Uygun",
  maintenance: "Bakımda",
} as const;

const STATUS_PILL_STYLES = {
  available: "bg-success-soft text-success",
  maintenance: "bg-warning-soft text-warning",
} as const;

function StatusPill({ status }: { status: Vehicle["maintenanceStatus"] }) {
  return (
    <span
      className={cn("rounded-full px-2 py-0.5 text-xs font-semibold", STATUS_PILL_STYLES[status])}
    >
      {STATUS_LABELS[status]}
    </span>
  );
}

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
        <div className="rounded-lg border">
          <Table>
            <TableHeader className="bg-muted/50">
              <TableRow>
                <TableHead>Plaka</TableHead>
                <TableHead>Model</TableHead>
                <TableHead>Durum</TableHead>
                <TableHead className="text-right">İşlemler</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {vehicles.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={4} className="text-muted-foreground text-center">
                    Kayıtlı araç bulunamadı.
                  </TableCell>
                </TableRow>
              ) : (
                vehicles.map((vehicle) =>
                  editingId === vehicle.id ? (
                    <TableRow key={vehicle.id}>
                      <TableCell>
                        <Input
                          value={editPlate}
                          onChange={(event) => setEditPlate(event.target.value)}
                        />
                      </TableCell>
                      <TableCell>
                        <Input
                          value={editModel}
                          onChange={(event) => setEditModel(event.target.value)}
                        />
                      </TableCell>
                      <TableCell>
                        <StatusPill status={vehicle.maintenanceStatus} />
                      </TableCell>
                      <TableCell className="text-right">
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
                      </TableCell>
                    </TableRow>
                  ) : (
                    <TableRow key={vehicle.id}>
                      <TableCell className="font-medium">{vehicle.plate}</TableCell>
                      <TableCell className="text-muted-foreground">{vehicle.model ?? "—"}</TableCell>
                      <TableCell>
                        <StatusPill status={vehicle.maintenanceStatus} />
                      </TableCell>
                      <TableCell className="text-right">
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
                      </TableCell>
                    </TableRow>
                  )
                )
              )}
            </TableBody>
          </Table>
        </div>
      )}
    </div>
  );
}
