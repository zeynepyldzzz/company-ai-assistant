import { useEffect, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { MapContainer, TileLayer, CircleMarker, Marker, Polyline, useMap } from "react-leaflet";
import type { LatLngBoundsExpression, LatLngTuple } from "leaflet";
import "leaflet/dist/leaflet.css";
import { MapPin, LocateFixed, Phone } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { cn } from "@/lib/utils";
import { createPinIcon } from "@/lib/map-pin-icon";
import { useAuth } from "@/auth/auth-context";
import { ApiError } from "@/api/client";
import { useDebouncedValue } from "@/hooks/use-debounced-value";
import {
  listShuttleRoutes,
  getShuttleStops,
  getShuttleRouteGeometry,
  getShuttleRecommendation,
  getAddressSuggestions,
} from "@/api/shuttle";

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

const searchPinIcon = createPinIcon("#dc2626", "#7f1d1d");
const highlightedStopPinIcon = createPinIcon("#16a34a", "#14532d");

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
  const [selectedLocation, setSelectedLocation] = useState<{ lat: number; lng: number } | null>(null);
  const [isAddressInputFocused, setIsAddressInputFocused] = useState(false);
  const [highlightedStopId, setHighlightedStopId] = useState<number | null>(null);
  const [searchedLocation, setSearchedLocation] = useState<LatLngTuple | null>(null);
  const [geolocationError, setGeolocationError] = useState<string | null>(null);

  const debouncedAddressInput = useDebouncedValue(addressInput);

  // A-33: kullanici yazarken oneri dropdown'u; belirsiz adreslerin tek tahminle
  // yanlis geocode edilmesi yerine kullanici listeden dogru sonucu secer.
  const addressSuggestionsQuery = useQuery({
    queryKey: ["shuttle-address-suggestions", debouncedAddressInput],
    queryFn: () => getAddressSuggestions(debouncedAddressInput.trim(), token!),
    enabled: Boolean(token) && debouncedAddressInput.trim().length >= 2 && !selectedLocation,
  });

  const addressSuggestions = addressSuggestionsQuery.data ?? [];
  const showAddressSuggestions = isAddressInputFocused && !selectedLocation && addressSuggestions.length > 0;

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

  // B-28: harita cizgisi duraklar arasi duz cizgi yerine gercek yol
  // geometrisini takip etsin (OSRM). Sunucu erisilemezse duz cizgiye fallback.
  const geometryQuery = useQuery({
    queryKey: ["shuttle-geometry", selectedRouteId],
    queryFn: () => getShuttleRouteGeometry(selectedRouteId!, token!),
    enabled: Boolean(token) && selectedRouteId !== null,
  });

  const recommendationMutation = useMutation({
    mutationFn: (params: { lat: number; lng: number } | { address: string }) =>
      getShuttleRecommendation(params, token!),
    onSuccess: (data) => {
      setSelectedRouteId(data.routeId);
      setHighlightedStopId(data.stopId);
      setSearchedLocation([data.searchLat, data.searchLng]);
      setAddressInput("");
      setSelectedLocation(null);
    },
  });

  const handleAddressInputChange = (value: string) => {
    setAddressInput(value);
    setSelectedLocation(null);
    setIsAddressInputFocused(true);
  };

  const handleSelectSuggestion = (suggestion: { label: string; lat: number; lng: number }) => {
    setAddressInput(suggestion.label);
    setSelectedLocation({ lat: suggestion.lat, lng: suggestion.lng });
    setIsAddressInputFocused(false);
    recommendationMutation.mutate({ lat: suggestion.lat, lng: suggestion.lng });
  };

  const handleFindRoute = () => {
    setIsAddressInputFocused(false);
    if (selectedLocation) {
      recommendationMutation.mutate(selectedLocation);
      return;
    }
    const address = addressInput.trim();
    if (!address) return;
    recommendationMutation.mutate({ address });
  };

  // B-33: kullanici adres yazmadan tarayici konumundan (GPS) en yakin servisi bulabilsin.
  const handleUseMyLocation = () => {
    setGeolocationError(null);
    setIsAddressInputFocused(false);
    if (!("geolocation" in navigator)) {
      setGeolocationError("Tarayıcınız konum özelliğini desteklemiyor.");
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (position) => {
        const { latitude, longitude } = position.coords;
        setAddressInput("");
        setSelectedLocation({ lat: latitude, lng: longitude });
        recommendationMutation.mutate({ lat: latitude, lng: longitude });
      },
      (error) => {
        if (error.code === error.PERMISSION_DENIED) {
          setGeolocationError("Konum izni reddedildi. Devam etmek için tarayıcı ayarlarından izin verin.");
        } else if (error.code === error.POSITION_UNAVAILABLE) {
          setGeolocationError("Konumunuz alınamadı.");
        } else if (error.code === error.TIMEOUT) {
          setGeolocationError("Konum isteği zaman aşımına uğradı.");
        } else {
          setGeolocationError("Konum alınamadı.");
        }
      },
      { enableHighAccuracy: true, timeout: 10_000 }
    );
  };

  const selectedRoute = routes.find((route) => route.id === selectedRouteId) ?? null;
  const stops = [...(stopsQuery.data ?? [])].sort((a, b) => a.orderIndex - b.orderIndex);
  const stopsWithCoords = stops.filter(
    (stop): stop is typeof stop & { latitude: number; longitude: number } =>
      stop.latitude !== null && stop.longitude !== null
  );
  const straightLinePositions: LatLngTuple[] = stopsWithCoords.map((stop) => [stop.latitude, stop.longitude]);
  const routeGeometryPositions: LatLngTuple[] =
    geometryQuery.data?.coordinates.map((c): LatLngTuple => [c.lat, c.lng]) ?? [];
  const polylinePositions = routeGeometryPositions.length >= 2 ? routeGeometryPositions : straightLinePositions;
  const isRecommendedRoute =
    searchedLocation !== null && recommendationMutation.data?.routeId === selectedRouteId;
  const bounds: LatLngBoundsExpression | null =
    polylinePositions.length > 0
      ? isRecommendedRoute && searchedLocation
        ? [...polylinePositions, searchedLocation]
        : polylinePositions
      : null;
  const departureTime = stops[0]?.time ?? null;

  return (
    <div className="space-y-4">
      <div className="bg-primary text-primary-foreground flex flex-col gap-3 rounded-lg p-4 sm:flex-row sm:items-center">
        <div className="flex shrink-0 items-center gap-2 text-sm font-semibold">
          <MapPin className="size-4" />
          Konumunu Yaz
        </div>
        <div className="relative flex-1">
          <Input
            value={addressInput}
            onChange={(e) => handleAddressInputChange(e.target.value)}
            onFocus={() => setIsAddressInputFocused(true)}
            onBlur={() => setIsAddressInputFocused(false)}
            onKeyDown={(e) => {
              if (e.key === "Enter") handleFindRoute();
              if (e.key === "Escape") setIsAddressInputFocused(false);
            }}
            placeholder="Konumunu yaz (örn. Bornova, Karşıyaka)…"
            className="bg-background text-foreground dark:bg-background dark:text-foreground w-full"
          />
          {showAddressSuggestions && (
            <ul className="bg-popover text-popover-foreground absolute z-[1100] mt-1 w-full overflow-hidden rounded-md border shadow-md">
              {addressSuggestions.map((suggestion, index) => (
                <li key={`${suggestion.lat}-${suggestion.lng}-${index}`}>
                  <button
                    type="button"
                    className="hover:bg-muted block w-full px-3 py-2 text-left text-sm"
                    onMouseDown={(e) => e.preventDefault()}
                    onClick={() => handleSelectSuggestion(suggestion)}
                  >
                    {suggestion.label}
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
        <Button
          type="button"
          variant="secondary"
          size="icon"
          onClick={handleUseMyLocation}
          disabled={recommendationMutation.isPending}
          title="Konumumu Kullan"
          aria-label="Konumumu Kullan"
        >
          <LocateFixed className="size-4" />
        </Button>
        <Button
          type="button"
          variant="secondary"
          onClick={handleFindRoute}
          disabled={recommendationMutation.isPending || !addressInput.trim()}
        >
          {recommendationMutation.isPending ? "Aranıyor…" : "En Yakını Bul"}
        </Button>
      </div>

      {geolocationError && <p className="text-destructive text-sm">{geolocationError}</p>}

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
                setSearchedLocation(null);
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
                scrollWheelZoom
                className="h-full min-h-[420px] w-full"
              >
                <TileLayer
                  attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> katkıda bulunanlar'
                  url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                />
                <FitBounds bounds={bounds} />
                <Polyline
                  positions={polylinePositions}
                  pathOptions={
                    isRecommendedRoute
                      ? { color: "#16a34a", weight: 6 }
                      : { color: "#2563eb", weight: 4 }
                  }
                />
                {stopsWithCoords.map((stop) =>
                  stop.id === highlightedStopId ? (
                    <Marker key={stop.id} position={[stop.latitude, stop.longitude]} icon={highlightedStopPinIcon} />
                  ) : (
                    <CircleMarker
                      key={stop.id}
                      center={[stop.latitude, stop.longitude]}
                      radius={7}
                      pathOptions={{ color: "#2563eb", fillColor: "#2563eb", fillOpacity: 0.9 }}
                    />
                  )
                )}
                {isRecommendedRoute && searchedLocation && (
                  <Marker position={searchedLocation} icon={searchPinIcon} />
                )}
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
                        onClick={() =>
                          setHighlightedStopId(stop.id === highlightedStopId ? null : stop.id)
                        }
                        className={cn(
                          "hover:bg-muted/50 flex cursor-pointer items-center justify-between gap-3 rounded-md py-2 px-2 transition-colors",
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
