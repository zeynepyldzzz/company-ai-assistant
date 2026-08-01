import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Plus, Pencil } from "lucide-react";
import type { ShuttleRoute } from "@company/shared";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetFooter,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";
import { useAuth } from "@/auth/auth-context";
import { ApiError } from "@/api/client";
import { createShuttleRoute, getShuttleStops, updateShuttleRoute } from "@/api/shuttle";
import { StopsEditor, emptyStopDraft, toStopRequests, type StopDraft } from "./stops-editor";

// B-17: tek form hem POST hem PUT icin (route prop'u varsa duzenleme), B-14
// (soför alanlari) ve B-15 (silme) ile ayni ekranda yonetilir.
export function ShuttleRouteFormSheet({ route }: { route?: ShuttleRoute }) {
  const { token } = useAuth();
  const queryClient = useQueryClient();
  const isEdit = Boolean(route);

  const [open, setOpen] = useState(false);
  const [name, setName] = useState(route?.name ?? "");
  const [plateNumber, setPlateNumber] = useState(route?.plateNumber ?? "");
  const [driverName, setDriverName] = useState(route?.driverName ?? "");
  const [driverPhone, setDriverPhone] = useState(route?.driverPhone ?? "");
  const [stops, setStops] = useState<StopDraft[]>([emptyStopDraft()]);

  // Duraklar liste yanitinda gelmiyor (ShuttleRoute), duzenlemede acilinca ayrica cekilir.
  const stopsQuery = useQuery({
    queryKey: ["shuttle-stops", route?.id],
    queryFn: () => getShuttleStops(route!.id, token!),
    enabled: Boolean(token) && Boolean(route) && open,
  });

  useEffect(() => {
    if (!open) return;
    setName(route?.name ?? "");
    setPlateNumber(route?.plateNumber ?? "");
    setDriverName(route?.driverName ?? "");
    setDriverPhone(route?.driverPhone ?? "");
    if (!isEdit) setStops([emptyStopDraft()]);
  }, [open, route, isEdit]);

  useEffect(() => {
    if (!stopsQuery.data) return;
    const sorted = [...stopsQuery.data].sort((a, b) => a.orderIndex - b.orderIndex);
    setStops(
      sorted.map((stop) => ({
        name: stop.name,
        time: stop.time?.slice(0, 5) ?? "",
        latitude: stop.latitude !== null ? String(stop.latitude) : "",
        longitude: stop.longitude !== null ? String(stop.longitude) : "",
      }))
    );
  }, [stopsQuery.data]);

  const mutation = useMutation({
    mutationFn: () => {
      const body = {
        name: name.trim(),
        plateNumber: plateNumber.trim() || null,
        driverName: driverName.trim() || null,
        driverPhone: driverPhone.trim() || null,
        stops: toStopRequests(stops),
      };
      return isEdit ? updateShuttleRoute(route!.id, body, token!) : createShuttleRoute(body, token!);
    },
    onSuccess: () => {
      toast.success(isEdit ? "Güzergah güncellendi." : "Güzergah oluşturuldu.");
      queryClient.invalidateQueries({ queryKey: ["shuttle-routes"] });
      queryClient.invalidateQueries({ queryKey: ["shuttle-stops"] });
      setOpen(false);
    },
    onError: (error) => {
      const message = error instanceof ApiError ? error.message : "İşlem başarısız oldu.";
      toast.error(message);
    },
  });

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    if (!name.trim()) {
      toast.error("Güzergah adı boş olamaz.");
      return;
    }
    const cleanedStops = toStopRequests(stops);
    if (cleanedStops.length === 0) {
      toast.error("En az bir durak eklenmeli.");
      return;
    }
    if (cleanedStops.some((s) => !s.name || !s.time)) {
      toast.error("Her durak için ad ve saat girilmeli.");
      return;
    }
    mutation.mutate();
  }

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      <SheetTrigger
        render={
          isEdit ? (
            <Button variant="outline" size="sm">
              <Pencil />
              Düzenle
            </Button>
          ) : (
            <Button>
              <Plus />
              Yeni Güzergah Ekle
            </Button>
          )
        }
      />
      <SheetContent className="w-full overflow-y-auto sm:max-w-lg">
        <SheetHeader>
          <SheetTitle>{isEdit ? "Güzergahı Düzenle" : "Yeni Güzergah"}</SheetTitle>
          <SheetDescription>Yalnızca shuttle_admin / system_admin erişebilir (FR-73).</SheetDescription>
        </SheetHeader>

        <form onSubmit={handleSubmit} className="flex flex-1 flex-col gap-4 px-4">
          <div className="space-y-1.5">
            <Label htmlFor="route-name">Güzergah Adı</Label>
            <Input id="route-name" value={name} onChange={(event) => setName(event.target.value)} />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="route-plate">Plaka</Label>
            <Input
              id="route-plate"
              value={plateNumber}
              onChange={(event) => setPlateNumber(event.target.value)}
              placeholder="34 ABC 123"
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="route-driver-name">Şoför Adı</Label>
            <Input
              id="route-driver-name"
              value={driverName}
              onChange={(event) => setDriverName(event.target.value)}
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="route-driver-phone">Şoför Telefonu</Label>
            <Input
              id="route-driver-phone"
              value={driverPhone}
              onChange={(event) => setDriverPhone(event.target.value)}
            />
          </div>

          {stopsQuery.isLoading && (
            <p className="text-muted-foreground text-sm">Duraklar yükleniyor…</p>
          )}
          <StopsEditor stops={stops} onChange={setStops} />

          <SheetFooter className="px-0">
            <Button type="submit" disabled={mutation.isPending}>
              {mutation.isPending ? "Kaydediliyor…" : isEdit ? "Güncelle" : "Oluştur"}
            </Button>
          </SheetFooter>
        </form>
      </SheetContent>
    </Sheet>
  );
}
