import { useMutation } from "@tanstack/react-query";
import { MapPin } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/auth/auth-context";
import { useGeolocation } from "@/hooks/use-geolocation";
import { ApiError } from "@/api/client";
import { getShuttleRecommendation } from "@/api/shuttle";

export function ShuttleRecommendationPage() {
  const { token } = useAuth();
  const { status, error: locationError, requestLocation } = useGeolocation();

  const recommendationMutation = useMutation({
    mutationFn: (coords: { lat: number; lng: number }) =>
      getShuttleRecommendation(coords.lat, coords.lng, token!),
  });

  const handleFindRoute = async () => {
    const coords = await requestLocation();
    if (coords) {
      recommendationMutation.mutate(coords);
    }
  };

  const isBusy = status === "locating" || recommendationMutation.isPending;

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold">Bana En Yakın Servis</h1>
      <p className="text-muted-foreground text-sm">
        Konumunuza göre en yakın servis durağını ve güzergahını önerelim.
      </p>

      <Button type="button" onClick={handleFindRoute} disabled={isBusy}>
        <MapPin className="size-4" />
        {isBusy ? "Konum alınıyor…" : "Konumumu Kullan"}
      </Button>

      {locationError && <p className="text-destructive text-sm">{locationError}</p>}

      {recommendationMutation.isError && (
        <p className="text-destructive text-sm">
          {recommendationMutation.error instanceof ApiError
            ? recommendationMutation.error.message
            : "Öneri alınamadı."}
        </p>
      )}

      {recommendationMutation.data && (
        <div className="space-y-2 rounded-lg border p-4">
          <h2 className="text-sm font-semibold">{recommendationMutation.data.routeName}</h2>
          <p className="text-muted-foreground text-sm">
            Plaka:{" "}
            <span className="text-foreground font-medium">
              {recommendationMutation.data.plateNumber ?? "Atanmamış"}
            </span>
          </p>
          <p className="text-muted-foreground text-sm">
            En yakın durak:{" "}
            <span className="text-foreground font-medium">{recommendationMutation.data.stopName}</span>
          </p>
          <p className="text-muted-foreground text-sm">
            Mesafe: {recommendationMutation.data.distanceKm.toFixed(1)} km · Tahmini süre:{" "}
            {recommendationMutation.data.estimatedMinutes} dk
          </p>
        </div>
      )}
    </div>
  );
}
