import { useEffect, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { MapContainer, TileLayer, CircleMarker, Polyline, useMap } from "react-leaflet";
import type { LatLngBoundsExpression, LatLngTuple } from "leaflet";
import "leaflet/dist/leaflet.css";
import { MapPin, Phone } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { cn } from "@/lib/utils";
import { useAuth } from "@/auth/auth-context";
import { ApiError } from "@/api/client";
import { listShuttleRoutes, getShuttleStops, getShuttleRecommendation } from "@/api/shuttle";

const ALL_ROUTES_PAGE_SIZE = 100;

function driverInitials(name: string | null): string {
  if (!name) return "?";
  return name
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join("");
}

function FitBounds({ bounds }: { bounds: LatLngBoundsExpression | null }) {
  const map = useMap();
  useEffect(() => {
    if (bounds) map.fitBounds(bounds, { padding: [32, 32] });
  }, [map, bounds]);
  return null;
}

export function ShuttlePage() {
  const { token } = useAuth();
  const [selectedRouteId, setSelectedRouteId] = useState<number | null>(null);
  const [addressInput, setAddressInput] = useState("");
  const [highlightedStopId, setHighlightedStopId] = useState<number | null>(null);

  const routesQuery = useQuery({
    queryKey: ["shuttle-routes-all"],
    queryFn: () => listShuttleRoutes({ page: 0, pageSize: ALL_ROUTES_PAGE_SIZE }, token!),
    enabled: Boolean(token),
  });

  const routes = routesQuery.data?.data ?? [];

  useEffect(() => {
    if (selectedRouteId === null && routes.length > 0) {
      setSelectedRouteId(routes[0].id);
    }
  }, [routes, selectedRouteId]);

  const stopsQuery = useQuery({
    queryKey: ["shuttle-stops", selectedRouteId],
    queryFn: () => getShuttleStops(selectedRouteId!, token!),
    enabled: Boolean(token) && selectedRouteId !== null,
  });

  const recommendationMutation = useMutation({
    mutationFn: (address: string) => getShuttleRecommendation({ address }, token!),
    onSuccess: (data) => {
      setSelectedRouteId(data.routeId);
      setHighlightedStopId(data.stopId);
    },
  });

  const handleFindRoute = () => {
    const address = addressInput.trim();
    if (!address) return;
    recommendationMutation.mutate(address);
  };

  const selectedRoute = routes.find((route) => route.id === selectedRouteId) ?? null;
  const stops = [...(stopsQuery.data ?? [])].sort((a, b) => a.orderIndex - b.orderIndex);
  const stopsWithCoords = stops.filter(
    (stop): stop is typeof stop & { latitude: number; longitude: number } =>
      stop.latitude !== null && stop.longitude !== null
  );
  const polylinePositions: LatLngTuple[] = stopsWithCoords.map((stop) => [stop.latitude, stop.longitude]);
  const bounds: LatLngBoundsExpression | null = polylinePositions.length > 0 ? polylinePositions : null;
  const departureTime = stops[0]?.time ?? null;

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold">Servisler</h1>

      <div className="bg-primary text-primary-foreground flex flex-col gap-3 rounded-lg p-4 sm:flex-row sm:items-center">
        <div className="flex shrink-0 items-center gap-2 text-sm font-semibold">
          <MapPin className="size-4" />
          Konumunu Yaz
        </div>
        <Input
          value={addressInput}
          onChange={(e) => setAddressInput(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") handleFindRoute();
          }}
          placeholder="Konumunu yaz (örn. Kadıköy, Levent)…"
          className="bg-background text-foreground flex-1"
        />
        <Button
          type="button"
          variant="secondary"
          onClick={handleFindRoute}
          disabled={recommendationMutation.isPending || !addressInput.trim()}
        >
          {recommendationMutation.isPending ? "Aranıyor…" : "En Yakını Bul"}
        </Button>
      </div>

      {recommendationMutation.isError && (
        <p className="text-destructive text-sm">
          {recommendationMutation.error instanceof ApiError
            ? recommendationMutation.error.message
            : "Öneri alınamadı."}
        </p>
      )}

      {recommendationMutation.data && (
        <p className="text-muted-foreground text-sm">
          En yakın durak:{" "}
          <span className="text-foreground font-medium">{recommendationMutation.data.stopName}</span>
          {" · "}
          {recommendationMutation.data.routeName} · {recommendationMutation.data.distanceKm.toFixed(1)} km
        </p>
      )}

      {routesQuery.isLoading && <p className="text-muted-foreground text-sm">Yükleniyor…</p>}
      {routesQuery.isError && <p className="text-destructive text-sm">Servis güzergahları yüklenemedi.</p>}

      {routes.length > 0 && (
        <div className="flex gap-2 overflow-x-auto pb-1">
          {routes.map((route) => (
            <button
              key={route.id}
              type="button"
              onClick={() => {
                setSelectedRouteId(route.id);
                setHighlightedStopId(null);
              }}
              className={cn(
                "shrink-0 rounded-full border px-4 py-2 text-sm font-medium whitespace-nowrap transition-colors",
                route.id === selectedRouteId
                  ? "border-primary bg-primary text-primary-foreground"
                  : "hover:bg-muted"
              )}
            >
              {route.name}
            </button>
          ))}
        </div>
      )}

      {routesQuery.data && routes.length === 0 && (
        <p className="text-muted-foreground text-sm">Tanımlı servis güzergahı yok.</p>
      )}

      {selectedRoute && (
        <div className="grid grid-cols-1 gap-4 lg:grid-cols-[2fr_1fr]">
          <Card className="overflow-hidden p-0">
            {stopsQuery.isLoading && (
              <p className="text-muted-foreground p-4 text-sm">Yükleniyor…</p>
            )}
            {stopsQuery.isError && (
              <p className="text-destructive p-4 text-sm">Durak bilgisi yüklenemedi.</p>
            )}
            {stopsQuery.data && polylinePositions.length === 0 && (
              <p className="text-muted-foreground p-4 text-sm">Bu güzergah için konum bilgisi yok.</p>
            )}
            {polylinePositions.length > 0 && (
              <MapContainer
                key={selectedRoute.id}
                center={polylinePositions[0]}
                zoom={12}
                scrollWheelZoom={false}
                className="h-[420px] w-full"
              >
                <TileLayer
                  attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> katkıda bulunanlar'
                  url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                />
                <FitBounds bounds={bounds} />
                <Polyline positions={polylinePositions} pathOptions={{ color: "#2563eb", weight: 4 }} />
                {stopsWithCoords.map((stop) => (
                  <CircleMarker
                    key={stop.id}
                    center={[stop.latitude, stop.longitude]}
                    radius={stop.id === highlightedStopId ? 10 : 7}
                    pathOptions={{
                      color: stop.id === highlightedStopId ? "#16a34a" : "#2563eb",
                      fillColor: stop.id === highlightedStopId ? "#16a34a" : "#2563eb",
                      fillOpacity: 0.9,
                    }}
                  />
                ))}
              </MapContainer>
            )}
          </Card>

          <div className="space-y-4">
            <Card>
              <CardHeader>
                <CardTitle className="text-base">{selectedRoute.name}</CardTitle>
              </CardHeader>
              <CardContent className="flex flex-wrap gap-x-4 gap-y-1">
                <span>Plaka: {selectedRoute.plateNumber ?? "Atanmamış"}</span>
                {departureTime && <span>Kalkış: {departureTime}</span>}
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="text-base">Servis Şoförü</CardTitle>
              </CardHeader>
              <CardContent>
                {selectedRoute.driverName ? (
                  <div className="flex items-center gap-3">
                    <Avatar>
                      <AvatarFallback>{driverInitials(selectedRoute.driverName)}</AvatarFallback>
                    </Avatar>
                    <div>
                      <p className="text-foreground font-medium">{selectedRoute.driverName}</p>
                      {selectedRoute.driverPhone && (
                        <a
                          href={`tel:${selectedRoute.driverPhone}`}
                          className="text-muted-foreground hover:text-foreground flex items-center gap-1 text-sm"
                        >
                          <Phone className="size-3.5" />
                          {selectedRoute.driverPhone}
                        </a>
                      )}
                    </div>
                  </div>
                ) : (
                  <p className="text-muted-foreground text-sm">Şoför bilgisi girilmemiş.</p>
                )}
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="text-base">Servis Rotaları</CardTitle>
              </CardHeader>
              <CardContent>
                {stops.length === 0 ? (
                  <p className="text-muted-foreground text-sm">Bu güzergah için durak tanımlanmamış.</p>
                ) : (
                  <ul className="divide-y">
                    {stops.map((stop) => (
                      <li
                        key={stop.id}
                        className={cn(
                          "flex items-center justify-between gap-3 py-2",
                          stop.id === highlightedStopId && "text-primary font-medium"
                        )}
                      >
                        <span>
                          {stop.orderIndex}. {stop.name}
                        </span>
                        <span className="text-muted-foreground text-sm">{stop.time ?? "—"}</span>
                      </li>
                    ))}
                  </ul>
                )}
              </CardContent>
            </Card>
          </div>
        </div>
      )}
    </div>
  );
}
