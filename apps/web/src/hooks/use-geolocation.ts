import { useCallback, useState } from "react";

type GeolocationStatus = "idle" | "locating" | "error";

interface Coordinates {
  lat: number;
  lng: number;
}

export function useGeolocation() {
  const [status, setStatus] = useState<GeolocationStatus>("idle");
  const [error, setError] = useState<string | null>(null);

  const requestLocation = useCallback((): Promise<Coordinates | null> => {
    if (!navigator.geolocation) {
      setStatus("error");
      setError("Tarayıcınız konum servisini desteklemiyor.");
      return Promise.resolve(null);
    }

    setStatus("locating");
    setError(null);

    return new Promise((resolve) => {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          setStatus("idle");
          resolve({ lat: position.coords.latitude, lng: position.coords.longitude });
        },
        (geoError) => {
          const message =
            geoError.code === geoError.PERMISSION_DENIED
              ? "Konum izni reddedildi. Öneri gösterebilmek için konum paylaşımına izin vermeniz gerekir."
              : "Konumunuz alınamadı, lütfen tekrar deneyin.";
          setStatus("error");
          setError(message);
          resolve(null);
        },
        { enableHighAccuracy: true, timeout: 10000 }
      );
    });
  }, []);

  return { status, error, requestLocation };
}
