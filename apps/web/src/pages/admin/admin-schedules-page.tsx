import { useQuery } from "@tanstack/react-query";
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
        <div className="overflow-x-auto rounded-lg border">
          <table className="w-full text-sm">
            <thead className="bg-muted/50">
              <tr>
                <th className="px-4 py-2 text-left font-medium">Çalışan</th>
                {WORK_DAYS.map((day) => (
                  <th key={day} className="px-4 py-2 text-center font-medium">
                    {DAY_LABELS[day]}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y">
              {data.employees.length === 0 ? (
                <tr>
                  <td colSpan={6} className="text-muted-foreground px-4 py-3 text-center">
                    Bu hafta için kayıt bulunamadı.
                  </td>
                </tr>
              ) : (
                data.employees.map((emp) => {
                  const statusByDay = Object.fromEntries(
                    emp.days.map((d) => [d.day, d.status])
                  ) as Record<WorkDay, ScheduleStatus | undefined>;
                  return (
                    <tr key={emp.employeeId}>
                      <td className="px-4 py-2 font-medium">{emp.employeeName}</td>
                      {WORK_DAYS.map((day) => (
                        <td key={day} className="text-muted-foreground px-4 py-2 text-center">
                          {statusByDay[day] ? STATUS_LABELS[statusByDay[day]!] : "—"}
                        </td>
                      ))}
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}