import { useQuery } from "@tanstack/react-query";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { useAuth } from "@/auth/auth-context";
import { getAdminSchedules } from "@/api/schedule";
import type { ScheduleStatus, WorkDay } from "@company/shared";

const WORK_DAYS: WorkDay[] = ["monday", "tuesday", "wednesday", "thursday", "friday"];

const DAY_LABELS: Record<WorkDay, string> = {
  monday: "Pzt",
  tuesday: "Sal",
  wednesday: "Çar",
  thursday: "Per",
  friday: "Cum",
};

const STATUS_LABELS: Record<ScheduleStatus, string> = {
  office: "Ofiste",
  remote: "Uzaktan",
  leave: "İzinli",
};

export function AdminSchedulesPage() {
  const { token } = useAuth();

  const { data, isLoading, isError } = useQuery({
    queryKey: ["admin", "schedules"],
    queryFn: () => getAdminSchedules(token!),
    enabled: Boolean(token),
  });

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-xl font-semibold">Çalışan Çalışma Düzeni</h1>
        <p className="text-muted-foreground text-sm">
          Tüm çalışanların bu haftaki düzeni (salt-okunur).
        </p>
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
              {data.employees.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} className="text-muted-foreground text-center">
                    Bu hafta için kayıt bulunamadı.
                  </TableCell>
                </TableRow>
              ) : (
                data.employees.map((emp) => {
                  const statusByDay = Object.fromEntries(
                    emp.days.map((d) => [d.day, d.status])
                  ) as Record<WorkDay, ScheduleStatus | undefined>;
                  return (
                    <TableRow key={emp.employeeId}>
                      <TableCell className="font-medium">{emp.employeeName}</TableCell>
                      {WORK_DAYS.map((day) => (
                        <TableCell key={day} className="text-muted-foreground text-center">
                          {statusByDay[day] ? STATUS_LABELS[statusByDay[day]!] : "—"}
                        </TableCell>
                      ))}
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