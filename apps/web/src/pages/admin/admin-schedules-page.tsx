import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Input } from "@/components/ui/input";
import { Card, CardContent } from "@/components/ui/card";
import { useAuth } from "@/auth/auth-context";
import { getAdminSchedules } from "@/api/schedule";
import type { ScheduleStatus, WorkDay } from "@company/shared";
import { cn } from "@/lib/utils";

const STATUS_PILL_STYLES: Record<ScheduleStatus, string> = {
  office: "bg-success-soft text-success",
  remote: "bg-warning-soft text-warning",
  leave: "bg-danger-soft text-danger",
};

const WORK_DAYS: WorkDay[] = ["monday", "tuesday", "wednesday", "thursday", "friday"];

const DAY_LABELS: Record<WorkDay, string> = {
  monday: "Pzt",
  tuesday: "Sal",
  wednesday: "Çar",
  thursday: "Per",
  friday: "Cum",
};

// A-32 (#188): getDay() 0=Pazar. Gun adini locale'den turetmek yerine sabit dizi
// kullaniyoruz; tarayici dili degistiginde anahtarlar bozulmasin.
const DAY_KEYS = [
  "sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday",
] as const;

const TODAY_FULL_LABELS: Record<WorkDay, string> = {
  monday: "Pazartesi",
  tuesday: "Salı",
  wednesday: "Çarşamba",
  thursday: "Perşembe",
  friday: "Cuma",
};

const STATUS_LABELS: Record<ScheduleStatus, string> = {
  office: "Ofiste",
  remote: "Uzaktan",
  leave: "İzinli",
};

export function AdminSchedulesPage() {
  const { token } = useAuth();
  const [search, setSearch] = useState("");

  const { data, isLoading, isError } = useQuery({
    queryKey: ["admin", "schedules"],
    queryFn: () => getAdminSchedules(token!),
    enabled: Boolean(token),
  });

  const filtered =
    data?.employees.filter((emp) =>
      emp.employeeName.toLowerCase().includes(search.trim().toLowerCase())
    ) ?? [];

  // A-32 (#188): kartlar BUGUN her durumda kac KISI oldugunu gosterir.
  // Onceden ic ice dongu ile kisi-gun sayiliyordu: tek calisanin dort uzaktan gunu
  // "Uzaktan 4" olarak okunuyordu ve kart basligi bunu kisi sayisi gibi sunuyordu.
  // 15 calisanla sayilar 75'e kadar cikacakti.
  const todayKey = DAY_KEYS[new Date().getDay()];
  const isWorkday = (WORK_DAYS as string[]).includes(todayKey);

  const summary = { office: 0, remote: 0, leave: 0 };
  if (isWorkday) {
    for (const emp of data?.employees ?? []) {
      const today = emp.days.find((day) => day.day === todayKey);
      if (today) {
        summary[today.status] += 1;
      }
    }
  }

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-xl font-semibold">Çalışan Çalışma Düzeni</h1>
        <p className="text-muted-foreground text-sm">
          Tüm çalışanların bu haftaki düzeni (salt-okunur).{" "}
          {/* A-32: kartlarin neyi saydigi yazmiyordu; "Uzaktan 4" hem "4 kisi" hem
              "4 gun" olarak okunabiliyordu. Hafta sonunda sifirlar sessiz kalmasin. */}
          {isWorkday
            ? `Yukarıdaki sayılar bugünü (${TODAY_FULL_LABELS[todayKey as WorkDay]}) gösterir.`
            : "Bugün hafta sonu; çalışma düzeni yalnızca Pazartesi-Cuma için tutulduğundan sayılar boş."}
        </p>
      </div>

      <div className="grid grid-cols-1 items-stretch gap-3 sm:grid-cols-4">
        <Input
          placeholder="Çalışan ara…"
          className="h-[38px] sm:col-span-1"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
        />
        {(["office", "remote", "leave"] as ScheduleStatus[]).map((status) => (
          <Card key={status} className={cn("gap-0 py-0", STATUS_PILL_STYLES[status])}>
            <CardContent className="flex items-center justify-between px-3 py-2">
              <span className="text-xs font-medium opacity-80">{STATUS_LABELS[status]}</span>
              <span className="text-lg font-bold">{summary[status]}</span>
            </CardContent>
          </Card>
        ))}
      </div>

      {isLoading && <p className="text-muted-foreground text-sm">Yükleniyor…</p>}
      {isError && <p className="text-destructive text-sm">Çalışma düzenleri yüklenemedi.</p>}

      {data && (
        <div className="rounded-lg border">
          <Table>
            <TableHeader className="bg-muted/50">
              <TableRow>
                <TableHead>Çalışan</TableHead>
                {WORK_DAYS.map((day) => (
                  <TableHead key={day} className="text-center">
                    {DAY_LABELS[day]}
                  </TableHead>
                ))}
              </TableRow>
            </TableHeader>
            <TableBody>
              {filtered.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} className="text-muted-foreground text-center">
                    {data.employees.length === 0
                      ? "Bu hafta için kayıt bulunamadı."
                      : "Sonuç bulunamadı."}
                  </TableCell>
                </TableRow>
              ) : (
                filtered.map((emp) => {
                  const statusByDay = Object.fromEntries(
                    emp.days.map((d) => [d.day, d.status])
                  ) as Record<WorkDay, ScheduleStatus | undefined>;
                  return (
                    <TableRow key={emp.employeeId}>
                      <TableCell className="font-medium">{emp.employeeName}</TableCell>
                      {WORK_DAYS.map((day) => {
                        const status = statusByDay[day];
                        return (
                          <TableCell key={day} className="text-center">
                            {status ? (
                              <span
                                className={cn(
                                  "inline-block rounded-full px-2 py-0.5 text-xs font-semibold",
                                  STATUS_PILL_STYLES[status]
                                )}
                              >
                                {STATUS_LABELS[status]}
                              </span>
                            ) : (
                              <span className="text-muted-foreground">—</span>
                            )}
                          </TableCell>
                        );
                      })}
                    </TableRow>
                  );
                })
              )}
            </TableBody>
          </Table>
        </div>
      )}
    </div>
  );
}