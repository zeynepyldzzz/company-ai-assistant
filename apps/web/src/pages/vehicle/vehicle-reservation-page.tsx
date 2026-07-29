import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Car } from "lucide-react";
import type { Reservation } from "@company/shared";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { useAuth } from "@/auth/auth-context";
import { ApiError } from "@/api/client";
import { cancelReservation, createReservation, listMyReservations, listVehicles } from "@/api/vehicle";

function formatDateTime(value: string): string {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString("tr-TR");
}

// datetime-local input'un min degeri icin yerel saatte "YYYY-MM-DDTHH:mm" formatlar.
function nowLocalDateTimeValue(): string {
  const now = new Date();
  now.setSeconds(0, 0);
  now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
  return now.toISOString().slice(0, 16);
}

function ReservationStatusBadge({ status }: { status: Reservation["status"] }) {
  const isConfirmed = status === "confirmed";
  return (
    <span
      className={`rounded-full px-2 py-0.5 text-xs font-semibold ${
        isConfirmed ? "bg-success-soft text-success" : "bg-muted text-muted-foreground"
      }`}
    >
      {isConfirmed ? "Onaylı" : "İptal Edildi"}
    </span>
  );
}

export function VehicleReservationPage() {
  const { token } = useAuth();
  const queryClient = useQueryClient();

  const [vehicleId, setVehicleId] = useState<number | null>(null);
  const [startTime, setStartTime] = useState("");
  const [endTime, setEndTime] = useState("");

  const vehiclesQuery = useQuery({
    queryKey: ["vehicles", "available"],
    queryFn: () => listVehicles(token!, true),
    enabled: Boolean(token),
  });

  const reservationsQuery = useQuery({
    queryKey: ["reservations", "me"],
    queryFn: () => listMyReservations(token!),
    enabled: Boolean(token),
  });

  const createMutation = useMutation({
    mutationFn: () => createReservation({ vehicleId: vehicleId!, startTime, endTime }, token!),
    onSuccess: () => {
      toast.success("Rezervasyon oluşturuldu.");
      queryClient.invalidateQueries({ queryKey: ["reservations", "me"] });
      setVehicleId(null);
      setStartTime("");
      setEndTime("");
    },
    onError: (error) => {
      const message = error instanceof ApiError ? error.message : "Rezervasyon oluşturulamadı.";
      toast.error(message);
    },
  });

  const cancelMutation = useMutation({
    mutationFn: (id: number) => cancelReservation(id, token!),
    onSuccess: () => {
      toast.success("Rezervasyon iptal edildi.");
      queryClient.invalidateQueries({ queryKey: ["reservations", "me"] });
    },
    onError: (error) => {
      const message = error instanceof ApiError ? error.message : "Rezervasyon iptal edilemedi.";
      toast.error(message);
    },
  });

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    if (vehicleId === null) {
      toast.error("Araç seçilmeli.");
      return;
    }
    if (!startTime || !endTime) {
      toast.error("Başlangıç ve bitiş zamanı zorunlu.");
      return;
    }
    if (new Date(startTime) < new Date()) {
      toast.error("Başlangıç zamanı geçmişte olamaz.");
      return;
    }
    if (new Date(endTime) <= new Date(startTime)) {
      toast.error("Bitiş zamanı başlangıç zamanından sonra olmalıdır.");
      return;
    }
    createMutation.mutate();
  }

  function handleCancel(reservation: Reservation) {
    if (window.confirm(`${reservation.vehiclePlate} plakalı araç rezervasyonu iptal edilecek. Onaylıyor musunuz?`)) {
      cancelMutation.mutate(reservation.id);
    }
  }

  return (
    <div className="max-w-2xl space-y-6">
      <div>
        <h1 className="text-xl font-semibold">Araç Rezervasyonu</h1>
        <p className="text-muted-foreground text-sm">
          Uygun şirket araçlarını görüntüle, rezervasyon oluştur veya iptal et.
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Yeni Rezervasyon</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-1.5">
              <Label>Araç</Label>
              {vehiclesQuery.isLoading && (
                <p className="text-muted-foreground text-sm">Araçlar yükleniyor…</p>
              )}
              {vehiclesQuery.isError && (
                <p className="text-destructive text-sm">Araçlar yüklenemedi.</p>
              )}
              {vehiclesQuery.data && vehiclesQuery.data.length === 0 && (
                <p className="text-muted-foreground text-sm">Şu anda uygun araç yok.</p>
              )}
              {vehiclesQuery.data && vehiclesQuery.data.length > 0 && (
                <Select<number | null> value={vehicleId} onValueChange={setVehicleId}>
                  <SelectTrigger>
                    <SelectValue placeholder="Araç seçin…" />
                  </SelectTrigger>
                  <SelectContent>
                    {vehiclesQuery.data.map((vehicle) => (
                      <SelectItem key={vehicle.id} value={vehicle.id}>
                        {vehicle.plate}
                        {vehicle.model ? ` — ${vehicle.model}` : ""}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label htmlFor="reservation-start">Başlangıç</Label>
                <Input
                  id="reservation-start"
                  type="datetime-local"
                  value={startTime}
                  min={nowLocalDateTimeValue()}
                  onChange={(event) => setStartTime(event.target.value)}
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="reservation-end">Bitiş</Label>
                <Input
                  id="reservation-end"
                  type="datetime-local"
                  value={endTime}
                  onChange={(event) => setEndTime(event.target.value)}
                />
              </div>
            </div>

            <Button type="submit" disabled={createMutation.isPending}>
              <Car />
              {createMutation.isPending ? "Oluşturuluyor…" : "Rezervasyon Oluştur"}
            </Button>
          </form>
        </CardContent>
      </Card>

      <div className="space-y-2">
        <h2 className="text-sm font-semibold">Rezervasyonlarım</h2>

        {reservationsQuery.isLoading && (
          <p className="text-muted-foreground text-sm">Yükleniyor…</p>
        )}
        {reservationsQuery.isError && (
          <p className="text-destructive text-sm">Rezervasyonlar yüklenemedi.</p>
        )}
        {reservationsQuery.data && reservationsQuery.data.length === 0 && (
          <p className="text-muted-foreground text-sm">Henüz rezervasyonun yok.</p>
        )}
        {reservationsQuery.data && reservationsQuery.data.length > 0 && (
          <ul className="divide-y rounded-lg border">
            {reservationsQuery.data.map((reservation) => (
              <li key={reservation.id} className="flex items-center justify-between gap-3 px-4 py-3">
                <div className="space-y-0.5">
                  <p className="text-sm font-medium">{reservation.vehiclePlate}</p>
                  <p className="text-muted-foreground text-xs">
                    {formatDateTime(reservation.startTime)} — {formatDateTime(reservation.endTime)}
                  </p>
                </div>
                <div className="flex items-center gap-3">
                  <ReservationStatusBadge status={reservation.status} />
                  {reservation.status === "confirmed" && (
                    <Button
                      variant="destructive"
                      size="sm"
                      className="whitespace-nowrap"
                      onClick={() => handleCancel(reservation)}
                      disabled={cancelMutation.isPending}
                    >
                      İptal Et
                    </Button>
                  )}
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
