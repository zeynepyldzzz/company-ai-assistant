import { ArrowDown, ArrowUp, Plus, Trash2 } from "lucide-react";
import type { AdminShuttleStopRequest, RoutePoint } from "@company/shared";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { FIELD_LIMITS } from "@/lib/field-limits";
import { Label } from "@/components/ui/label";
import { cn } from "@/lib/utils";
import { RouteMapEditor, type MapMode } from "./route-map-editor";

// Formdaki tek durak satiri. orderIndex gonderirken dizideki konumdan otomatik
// atanir (asagi/yukari ile siralanir), burada tutulmaz.
export type StopDraft = { name: string; time: string; latitude: string; longitude: string };

export function emptyStopDraft(): StopDraft {
  return { name: "", time: "", latitude: "", longitude: "" };
}

// time input "HH:mm" doner, backend LocalTime "HH:mm:ss" bekliyor.
function toLocalTime(value: string): string {
  return value.length === 5 ? `${value}:00` : value;
}

export function toStopRequests(stops: StopDraft[]): AdminShuttleStopRequest[] {
  return stops
    .filter((s) => s.name.trim() || s.time.trim())
    .map((s, i) => ({
      name: s.name.trim(),
      time: toLocalTime(s.time.trim()),
      orderIndex: i + 1,
      latitude: s.latitude.trim() ? Number(s.latitude) : null,
      longitude: s.longitude.trim() ? Number(s.longitude) : null,
    }));
}

// B-30: durak konumu artik tek, paylasilan bir haritadan seciliyor. Hangi
// duragin haritada gosterilip duzenlendigini formu tutan ust bilesen
// (ShuttleRouteFormSheet) kontrol eder, boylece form her acildiginda /
// farkli bir guzergah duzenlenirken sifirlanabilir.
export function StopsEditor({
  stops,
  onChange,
  activeIndex,
  onActiveIndexChange,
  mapMode,
  onMapModeChange,
  draftPoints,
  onDraftPointsChange,
  matchedGeometry,
  onMatch,
  isMatching,
}: {
  stops: StopDraft[];
  onChange: (stops: StopDraft[]) => void;
  activeIndex: number | null;
  onActiveIndexChange: (index: number | null) => void;
  mapMode: MapMode;
  onMapModeChange: (mode: MapMode) => void;
  draftPoints: RoutePoint[];
  onDraftPointsChange: (points: RoutePoint[]) => void;
  matchedGeometry: RoutePoint[] | null;
  onMatch: () => void;
  isMatching: boolean;
}) {
  function updateStop(index: number, patch: Partial<StopDraft>) {
    onChange(stops.map((s, i) => (i === index ? { ...s, ...patch } : s)));
  }

  function moveStop(index: number, direction: -1 | 1) {
    const target = index + direction;
    if (target < 0 || target >= stops.length) return;
    const next = [...stops];
    [next[index], next[target]] = [next[target], next[index]];
    onChange(next);
    onActiveIndexChange(target);
  }

  function removeStop(index: number) {
    onChange(stops.filter((_, i) => i !== index));
    if (activeIndex === index) onActiveIndexChange(null);
    else if (activeIndex !== null && activeIndex > index) onActiveIndexChange(activeIndex - 1);
  }

  function addStop() {
    onChange([...stops, emptyStopDraft()]);
    onActiveIndexChange(stops.length);
  }

  const activeStop = activeIndex !== null ? (stops[activeIndex] ?? null) : null;
  const stopsWithCoords: RoutePoint[] = stops
    .filter((s) => s.latitude.trim() && s.longitude.trim())
    .map((s) => ({ lat: Number(s.latitude), lng: Number(s.longitude) }));

  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between">
        <Label>Duraklar</Label>
        <Button type="button" variant="outline" size="sm" onClick={addStop}>
          <Plus />
          Durak Ekle
        </Button>
      </div>
      {stops.length === 0 && (
        <p className="text-muted-foreground text-sm">En az bir durak eklenmeli.</p>
      )}
      <div className="grid grid-cols-1 gap-3 md:grid-cols-[1fr_1.4fr]">
        <div className="space-y-2">
          {stops.map((stop, index) => (
            <div
              key={index}
              onClick={() => onActiveIndexChange(index)}
              className={cn(
                "flex cursor-pointer items-start gap-2 rounded-lg border p-2 transition-colors",
                index === activeIndex ? "border-primary ring-primary/30 ring-1" : "hover:bg-muted/50"
              )}
            >
              <span className="text-muted-foreground pt-2 text-sm font-medium">{index + 1}.</span>
              <div className="flex-1 space-y-1.5">
                <div className="flex gap-1.5">
                  <Input
                    value={stop.name}
                    maxLength={FIELD_LIMITS.shuttleStopName}
                    onChange={(event) => updateStop(index, { name: event.target.value })}
                    placeholder="Durak adı"
                  />
                  <Input
                    type="time"
                    value={stop.time}
                    onChange={(event) => updateStop(index, { time: event.target.value })}
                    className="w-32 shrink-0"
                  />
                </div>
                <div className="flex gap-1.5">
                  <Input
                    type="number"
                    step="any"
                    value={stop.latitude}
                    onChange={(event) => updateStop(index, { latitude: event.target.value })}
                    placeholder="Enlem (opsiyonel)"
                  />
                  <Input
                    type="number"
                    step="any"
                    value={stop.longitude}
                    onChange={(event) => updateStop(index, { longitude: event.target.value })}
                    placeholder="Boylam (opsiyonel)"
                  />
                </div>
              </div>
              <div className="flex flex-col gap-1">
                <Button
                  type="button"
                  variant="ghost"
                  size="icon-sm"
                  disabled={index === 0}
                  onClick={(event) => {
                    event.stopPropagation();
                    moveStop(index, -1);
                  }}
                >
                  <ArrowUp />
                </Button>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon-sm"
                  disabled={index === stops.length - 1}
                  onClick={(event) => {
                    event.stopPropagation();
                    moveStop(index, 1);
                  }}
                >
                  <ArrowDown />
                </Button>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon-sm"
                  onClick={(event) => {
                    event.stopPropagation();
                    removeStop(index);
                  }}
                >
                  <Trash2 />
                </Button>
              </div>
            </div>
          ))}
        </div>

        <div className="min-h-[480px]">
          <RouteMapEditor
            mode={mapMode}
            onModeChange={onMapModeChange}
            activeStopLatitude={activeStop?.latitude ?? ""}
            activeStopLongitude={activeStop?.longitude ?? ""}
            onActiveStopChange={
              activeIndex !== null
                ? (latitude, longitude) => updateStop(activeIndex, { latitude, longitude })
                : () => {}
            }
            draftPoints={draftPoints}
            onDraftPointsChange={onDraftPointsChange}
            matchedGeometry={matchedGeometry}
            stopReferenceMarkers={stopsWithCoords}
            onMatch={onMatch}
            isMatching={isMatching}
          />
        </div>
      </div>
    </div>
  );
}
