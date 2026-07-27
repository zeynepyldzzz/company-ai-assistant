import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/auth/auth-context";
import { getMySchedule, saveMySchedule } from "@/api/schedule";
import type { ScheduleDay, ScheduleStatus, WorkDay } from "@company/shared";

const WORK_DAYS: WorkDay[] = ["monday", "tuesday", "wednesday", "thursday", "friday"];

const DAY_LABELS: Record<WorkDay, string> = {
  monday: "Pazartesi",
  tuesday: "Salı",
  wednesday: "Çarşamba",
  thursday: "Perşembe",
  friday: "Cuma",
};

const STATUS_OPTIONS: { value: ScheduleStatus; label: string }[] = [
  { value: "office", label: "Ofiste" },
  { value: "remote", label: "Uzaktan" },
  { value: "leave", label: "İzinli" },
];

// Ekrandaki secim durumu: her gun icin bir status (henuz secilmemisse null)
type Selection = Record<WorkDay, ScheduleStatus | null>;

const EMPTY_SELECTION: Selection = {
  monday: null,
  tuesday: null,
  wednesday: null,
  thursday: null,
  friday: null,
};

function toSelection(days: ScheduleDay[]): Selection {
  const selection = { ...EMPTY_SELECTION };
  for (const d of days) {
    selection[d.day] = d.status;
  }
  return selection;
}

function toDays(selection: Selection): ScheduleDay[] {
  return WORK_DAYS.map((day) => ({ day, status: selection[day]! }));
}
export function SchedulePage() {
  const { token } = useAuth();
  const queryClient = useQueryClient();
  const [selection, setSelection] = useState<Selection>(EMPTY_SELECTION);

  const scheduleQuery = useQuery({
    queryKey: ["schedule", "me"],
    queryFn: () => getMySchedule(token!),
    enabled: Boolean(token),
  });

  // API'den kayitli hafta gelince ekrandaki secimleri onunla doldur
  useEffect(() => {
    if (scheduleQuery.data) {
      setSelection(toSelection(scheduleQuery.data.days));
    }
  }, [scheduleQuery.data]);

  const saveMutation = useMutation({
    mutationFn: (days: ScheduleDay[]) => saveMySchedule(token!, days),
    onSuccess: () => {
      // Kayit basarili -> onbellekteki schedule verisini tazele
      queryClient.invalidateQueries({ queryKey: ["schedule"] });
    },
  });

  const allSelected = WORK_DAYS.every((day) => selection[day] !== null);

  const summary = {
    office: WORK_DAYS.filter((d) => selection[d] === "office").length,
    remote: WORK_DAYS.filter((d) => selection[d] === "remote").length,
    leave: WORK_DAYS.filter((d) => selection[d] === "leave").length,
  };

  function setDayStatus(day: WorkDay, status: ScheduleStatus) {
    setSelection((prev) => ({ ...prev, [day]: status }));
  }

  function handleSave() {
    if (!allSelected) return;
    saveMutation.mutate(toDays(selection));
  }
  return (
    <div className="max-w-xl space-y-4">
      <div>
        <h1 className="text-xl font-semibold">Haftalık Çalışma Düzenim</h1>
        <p className="text-muted-foreground text-sm">Her gün için nerede çalışacağını seç</p>
      </div>

      {scheduleQuery.isLoading && <p className="text-muted-foreground text-sm">Yükleniyor…</p>}
      {scheduleQuery.isError && (
        <p className="text-destructive text-sm">Çalışma düzeni yüklenemedi.</p>
      )}

      {scheduleQuery.data && (
        <div className="space-y-4 rounded-lg border p-4">
          <ul className="divide-y">
            {WORK_DAYS.map((day) => (
              <li key={day} className="flex items-center justify-between py-2.5">
                <span className="text-sm font-medium">{DAY_LABELS[day]}</span>
                <div className="inline-flex overflow-hidden rounded-md border">
                  {STATUS_OPTIONS.map((opt) => (
                    <button
                      key={opt.value}
                      type="button"
                      onClick={() => setDayStatus(day, opt.value)}
                      className={`px-3 py-1.5 text-xs transition-colors ${
                        selection[day] === opt.value
                          ? "bg-primary text-primary-foreground font-medium"
                          : "text-muted-foreground hover:bg-muted/50"
                      }`}
                    >
                      {opt.label}
                    </button>
                  ))}
                </div>
              </li>
            ))}
          </ul>

          <div className="grid grid-cols-3 gap-3">
            {STATUS_OPTIONS.map((opt) => (
              <div key={opt.value} className="rounded-md border px-3 py-2 text-center">
                <p className="text-muted-foreground text-xs">{opt.label}</p>
                <p className="text-lg font-semibold">{summary[opt.value]}</p>
              </div>
            ))}
          </div>

          <button
            type="button"
            onClick={handleSave}
            disabled={!allSelected || saveMutation.isPending}
            className="bg-primary text-primary-foreground w-full rounded-md py-2 text-sm font-medium transition-opacity disabled:opacity-50"
          >
            {saveMutation.isPending ? "Kaydediliyor…" : "Kaydet"}
          </button>

          {!allSelected && (
            <p className="text-muted-foreground text-center text-xs">
              Kaydetmek için 5 günün hepsini seç.
            </p>
          )}
          {saveMutation.isSuccess && (
            <p className="text-center text-xs text-green-600">Kaydedildi ✓</p>
          )}
          {saveMutation.isError && (
            <p className="text-destructive text-center text-xs">Kaydedilemedi, tekrar dene.</p>
          )}
        </div>
      )}
    </div>
  );
}