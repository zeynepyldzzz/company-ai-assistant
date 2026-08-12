import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Plus, Pencil } from "lucide-react";
import type { ShuttleRoute, RoutePoint } from "@company/shared";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { useAuth } from "@/auth/auth-context";
import { ApiError } from "@/api/client";
import {
  createShuttleRoute,
  getShuttleStops,
  getShuttleRouteGeometryPoints,
  matchRouteGeometry,
  updateShuttleRoute,
} from "@/api/shuttle";
import { StopsEditor, emptyStopDraft, toStopRequests, type StopDraft } from "./stops-editor";
import type { MapMode } from "./route-map-editor";

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
  const [activeStopIndex, setActiveStopIndex] = useState<number | null>(0);
  const [mapMode, setMapMode] = useState<MapMode>("stop");
  const [draftPoints, setDraftPoints] = useState<RoutePoint[]>([]);
  const [matchedGeometry, setMatchedGeometry] = useState<RoutePoint[] | null>(null);

  // Duraklar liste yanitinda gelmiyor (ShuttleRoute), duzenlemede acilinca ayrica cekilir.
  const stopsQuery = useQuery({
    queryKey: ["shuttle-stops", route?.id],
    queryFn: () => getShuttleStops(route!.id, token!),
    enabled: Boolean(token) && Boolean(route) && open,
  });

  // B-31: onceden cizilip kaydedilmis (varsa) rota noktalarini duzenleme
  // ekraninda geri yuklemek icin. Public /geometry ucundan FARKLI - o her
  // zaman bir cizgi doner (OSRM/duz-cizgi fallback'i ile), bu ise sadece
  // gercekten kaydedilmis noktalari doner (yoksa bos liste).
  const geometryPointsQuery = useQuery({
    queryKey: ["shuttle-geometry-points", route?.id],
    queryFn: () => getShuttleRouteGeometryPoints(route!.id, token!),
    enabled: Boolean(token) && Boolean(route) && open,
  });

  useEffect(() => {
    if (!open) return;
    setName(route?.name ?? "");
    setPlateNumber(route?.plateNumber ?? "");
    setDriverName(route?.driverName ?? "");
    setDriverPhone(route?.driverPhone ?? "");
    if (!isEdit) {
      setStops([emptyStopDraft()]);
      setActiveStopIndex(0);
      setMapMode("stop");
      setDraftPoints([]);
      setMatchedGeometry(null);
    }
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
    setActiveStopIndex(sorted.length > 0 ? 0 : null);
  }, [stopsQuery.data]);

  useEffect(() => {
    if (!geometryPointsQuery.data) return;
    if (geometryPointsQuery.data.length > 0) {
      setDraftPoints(geometryPointsQuery.data);
      setMatchedGeometry(geometryPointsQuery.data);
    }
  }, [geometryPointsQuery.data]);

  const matchMutation = useMutation({
    mutationFn: () => matchRouteGeometry(draftPoints, token!),
    onSuccess: (data) => setMatchedGeometry(data.coordinates),
    onError: (error) => {
      const message = error instanceof ApiError ? error.message : "Rota eşleştirilemedi.";
      toast.error(message);
    },
  });

  // Eslesme sonrasi taslak noktalar degisirse (yeni tik / son nokta sil /
  // temizle) onceki eslesme artik gecersiz - "sadece en son eslesen
  // kaydedilir" kurali burada uygulanir.
  function handleDraftPointsChange(points: RoutePoint[]) {
    setDraftPoints(points);
    setMatchedGeometry(null);
  }

  const mutation = useMutation({
    mutationFn: () => {
      const body = {
        name: name.trim(),
        plateNumber: plateNumber.trim() || null,
        driverName: driverName.trim() || null,
        driverPhone: driverPhone.trim() || null,
        stops: toStopRequests(stops),
        geometryPoints: matchedGeometry,
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
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger
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
      <DialogContent className="w-full overflow-y-auto sm:max-w-6xl">
        <DialogHeader>
          <DialogTitle>{isEdit ? "Güzergahı Düzenle" : "Yeni Güzergah"}</DialogTitle>
          <DialogDescription>
            Bu ekrana yalnızca Servis Yöneticisi ve Sistem Yöneticisi erişebilir.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="flex flex-1 flex-col gap-4 px-4">
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <div className="space-y-1.5">
              <Label htmlFor="route-name">Güzergah Adı</Label>
              <Input id="route-name" value={name} onChange={(event) => setName(event.target.value)} />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="route-plate">Plaka</Label>
              <Input
                id="route-plate"
                value={plateNumber}
                // Plaka her zaman BUYUK harf; kullanici kucuk yazsa da duzeltiyoruz, cunku
                // chatbot plaka eslestirmesi ve listeler bu degeri oldugu gibi gosteriyor.
                onChange={(event) => setPlateNumber(event.target.value.toUpperCase().slice(0, 12))}
                maxLength={12}
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
                inputMode="tel"
                maxLength={20}
                placeholder="0532 111 22 33"
                value={driverPhone}
                onChange={(event) => setDriverPhone(event.target.value.replace(/[^\d\s()+-]/g, ""))}
              />
            </div>
          </div>

          {stopsQuery.isLoading && (
            <p className="text-muted-foreground text-sm">Duraklar yükleniyor…</p>
          )}
          <StopsEditor
            stops={stops}
            onChange={setStops}
            activeIndex={activeStopIndex}
            onActiveIndexChange={setActiveStopIndex}
            mapMode={mapMode}
            onMapModeChange={setMapMode}
            draftPoints={draftPoints}
            onDraftPointsChange={handleDraftPointsChange}
            matchedGeometry={matchedGeometry}
            onMatch={() => matchMutation.mutate()}
            isMatching={matchMutation.isPending}
          />

          <DialogFooter className="px-0">
            <Button type="submit" disabled={mutation.isPending}>
              {mutation.isPending ? "Kaydediliyor…" : isEdit ? "Güncelle" : "Oluştur"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
