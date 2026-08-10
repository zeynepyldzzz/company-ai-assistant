import { useEffect, useRef, useState } from "react";
import { MapContainer, TileLayer, Marker, CircleMarker, Polyline, useMap, useMapEvents } from "react-leaflet";
import type { Marker as LeafletMarker, LatLngBoundsExpression, LatLngTuple } from "leaflet";
import "leaflet/dist/leaflet.css";
import { MapPin, PenLine } from "lucide-react";
import type { RoutePoint } from "@company/shared";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { createPinIcon } from "@/lib/map-pin-icon";

const stopPinIcon = createPinIcon("#2563eb", "#1e3a8a");

// Uygulama zaten Izmir/Manisa ile sinirli (bkz. adres onerileri, B-26);
// harita ilk acildiginda konum secilmemisse bu bolgeyi merkez alir.
const DEFAULT_CENTER: LatLngTuple = [38.45, 27.6];
const DEFAULT_ZOOM = 9;
const SELECTED_ZOOM = 15;

export type MapMode = "stop" | "draw";

function toTuple(point: RoutePoint): LatLngTuple {
  return [point.lat, point.lng];
}

function ClickHandler({ onClick }: { onClick: (lat: number, lng: number) => void }) {
  useMapEvents({
    click(event) {
      onClick(event.latlng.lat, event.latlng.lng);
    },
  });
  return null;
}

// B-30: aktif durak degistiginde (baska bir durak satirina tiklandiginda)
// haritayi o durogun konumuna (varsa) kaydirir - tek harita instance'i
// baska bir satir secildiginde yeniden yaratilmak yerine gorunumu kayar.
function RecenterOnChange({ position }: { position: LatLngTuple | null }) {
  const map = useMap();
  useEffect(() => {
    if (position) map.setView(position, SELECTED_ZOOM);
  }, [map, position]);
  return null;
}

// B-31: "Rota Cizimi" moduna girildiginde (her tiklamada degil - surekli
// refit kullaniciyla "kavga eder") mevcut duraklari kapsayan bir gorunume
// bir kere gecer, boylece admin cizime baslarken duraklari gorur.
function FitStopsOnDrawEntry({ mode, bounds }: { mode: MapMode; bounds: LatLngBoundsExpression | null }) {
  const map = useMap();
  useEffect(() => {
    if (mode === "draw" && bounds) {
      map.fitBounds(bounds, { padding: [32, 32] });
    }
    // bilerek sadece [mode] - her durak/bounds guncellemesinde degil,
    // sadece "draw" moduna girildigi anda bir kere calismali.
  }, [mode]);
  return null;
}

// B-31: tek harita instance'i, iki mod. "Durak Konumu": aktif duragin
// pinini tiklama/surukleme ile tasir (eski StopLocationPicker). "Rota
// Cizimi": tiklamalar taslak bir rota noktasi dizisine eklenir; "Yolu
// Eslestir" bu noktalari OSRM Match'e gonderip yola oturtulmus onizlemeyi
// (yesil cizgi) doner.
export function RouteMapEditor({
  mode,
  onModeChange,
  activeStopLatitude,
  activeStopLongitude,
  onActiveStopChange,
  draftPoints,
  onDraftPointsChange,
  matchedGeometry,
  stopReferenceMarkers,
  onMatch,
  isMatching,
}: {
  mode: MapMode;
  onModeChange: (mode: MapMode) => void;
  activeStopLatitude: string;
  activeStopLongitude: string;
  onActiveStopChange: (latitude: string, longitude: string) => void;
  draftPoints: RoutePoint[];
  onDraftPointsChange: (points: RoutePoint[]) => void;
  matchedGeometry: RoutePoint[] | null;
  stopReferenceMarkers: RoutePoint[];
  onMatch: () => void;
  isMatching: boolean;
}) {
  const markerRef = useRef<LeafletMarker>(null);
  // Ilk render'da haritayi merkezlemek icin - sonrasinda mod degisince
  // tekrar hesaplanmaz (MapContainer'in center prop'u mount sonrasi
  // kontrolsuz), o yuzden bilerek sadece ilk deger olarak kullanilir.
  const [initialCenter] = useState<LatLngTuple>(() => {
    const lat = activeStopLatitude.trim() ? Number(activeStopLatitude) : null;
    const lng = activeStopLongitude.trim() ? Number(activeStopLongitude) : null;
    return lat !== null && lng !== null && !Number.isNaN(lat) && !Number.isNaN(lng) ? [lat, lng] : DEFAULT_CENTER;
  });

  const lat = activeStopLatitude.trim() ? Number(activeStopLatitude) : null;
  const lng = activeStopLongitude.trim() ? Number(activeStopLongitude) : null;
  const activeStopPosition: LatLngTuple | null =
    lat !== null && lng !== null && !Number.isNaN(lat) && !Number.isNaN(lng) ? [lat, lng] : null;

  const stopReferencePositions = stopReferenceMarkers.map(toTuple);
  const draftPositions = draftPoints.map(toTuple);
  const matchedPositions = matchedGeometry?.map(toTuple) ?? null;
  const stopBounds: LatLngBoundsExpression | null =
    stopReferencePositions.length > 0 ? stopReferencePositions : null;

  function handleClick(clickLat: number, clickLng: number) {
    if (mode === "stop") {
      onActiveStopChange(clickLat.toFixed(6), clickLng.toFixed(6));
    } else {
      onDraftPointsChange([...draftPoints, { lat: clickLat, lng: clickLng }]);
    }
  }

  function handleDragEnd() {
    const marker = markerRef.current;
    if (!marker) return;
    const next = marker.getLatLng();
    onActiveStopChange(next.lat.toFixed(6), next.lng.toFixed(6));
  }

  return (
    <div className="flex h-full flex-col gap-2">
      <div className="flex flex-wrap items-center gap-2">
        <div className="flex overflow-hidden rounded-md border">
          <Button
            type="button"
            size="sm"
            variant="ghost"
            className={cn("rounded-none", mode === "stop" && "bg-primary text-primary-foreground")}
            onClick={() => onModeChange("stop")}
          >
            <MapPin />
            Durak Konumu
          </Button>
          <Button
            type="button"
            size="sm"
            variant="ghost"
            className={cn("rounded-none", mode === "draw" && "bg-primary text-primary-foreground")}
            onClick={() => onModeChange("draw")}
          >
            <PenLine />
            Rota Çizimi
          </Button>
        </div>
        {mode === "draw" && (
          <>
            <Button
              type="button"
              size="sm"
              variant="outline"
              disabled={draftPoints.length === 0}
              onClick={() => onDraftPointsChange(draftPoints.slice(0, -1))}
            >
              Son Noktayı Sil
            </Button>
            <Button
              type="button"
              size="sm"
              variant="outline"
              disabled={draftPoints.length === 0}
              onClick={() => onDraftPointsChange([])}
            >
              Temizle
            </Button>
            <Button
              type="button"
              size="sm"
              disabled={draftPoints.length < 2 || isMatching}
              onClick={onMatch}
            >
              {isMatching ? "Eşleştiriliyor…" : "Yolu Eşleştir"}
            </Button>
          </>
        )}
      </div>

      <div className="min-h-0 flex-1 overflow-hidden rounded-md border">
        <MapContainer center={initialCenter} zoom={activeStopPosition ? SELECTED_ZOOM : DEFAULT_ZOOM} scrollWheelZoom className="h-full min-h-[440px] w-full">
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> katkıda bulunanlar'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          <ClickHandler onClick={handleClick} />
          <FitStopsOnDrawEntry mode={mode} bounds={stopBounds} />

          {/* B-31: diger duraklarin konumu her iki modda da soluk referans
              nokta olarak gorunur - yeni durak eklerken oncekilere gore
              konumlandirma yapilabilsin diye. Aktif duragin kendi pozisyonu
              (buyuk pin) ile cakismasin diye o pozisyon disarida birakilir. */}
          {stopReferencePositions.map((position, index) => {
            if (
              mode === "stop" &&
              activeStopPosition &&
              position[0] === activeStopPosition[0] &&
              position[1] === activeStopPosition[1]
            ) {
              return null;
            }
            return (
              <CircleMarker
                key={index}
                center={position}
                radius={6}
                pathOptions={{ color: "#334155", fillColor: "#475569", fillOpacity: 0.85, weight: 2 }}
              />
            );
          })}

          {mode === "stop" && (
            <>
              <RecenterOnChange position={activeStopPosition} />
              {activeStopPosition && (
                <Marker
                  position={activeStopPosition}
                  icon={stopPinIcon}
                  draggable
                  ref={markerRef}
                  eventHandlers={{ dragend: handleDragEnd }}
                />
              )}
            </>
          )}

          {mode === "draw" && draftPositions.length > 0 && (
            <>
              <Polyline positions={draftPositions} pathOptions={{ color: "#2563eb", weight: 3, dashArray: "4 4" }} />
              {draftPositions.map((position, index) => (
                <CircleMarker
                  key={index}
                  center={position}
                  radius={5}
                  pathOptions={{ color: "#2563eb", fillColor: "#2563eb", fillOpacity: 0.9 }}
                />
              ))}
            </>
          )}

          {mode === "draw" && matchedPositions && matchedPositions.length > 0 && (
            <Polyline positions={matchedPositions} pathOptions={{ color: "#16a34a", weight: 6 }} />
          )}
        </MapContainer>
      </div>
    </div>
  );
}
