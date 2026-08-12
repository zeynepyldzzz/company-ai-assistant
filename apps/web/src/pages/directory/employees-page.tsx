import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { OfficeStatusSchema, type OfficeStatus } from "@company/shared";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Card, CardContent } from "@/components/ui/card";
import { PaginationControls } from "@/components/pagination-controls";
import { useDebouncedValue } from "@/hooks/use-debounced-value";
import { useAuth } from "@/auth/auth-context";
import { searchDepartments, searchEmployees } from "@/api/directory";
import { cn } from "@/lib/utils";

const PAGE_SIZE = 12;

const statusStyles: Record<OfficeStatus, string> = {
  Ofiste: "bg-success-soft text-success",
  Uzaktan: "bg-warning-soft text-warning",
  Izinde: "bg-danger-soft text-danger",
};

// B-32 (#204): gun secim barı - backend'in bekledigi kucuk harf Ingilizce anahtar (bkz.
// TodayStatusService) ile ekranda gosterilecek Turkce kisaltma.
const WEEKDAYS: { key: string; label: string }[] = [
  { key: "monday", label: "Pzt" },
  { key: "tuesday", label: "Sal" },
  { key: "wednesday", label: "Çar" },
  { key: "thursday", label: "Per" },
  { key: "friday", label: "Cum" },
];

function todayDayKey(): string | null {
  const key = new Date()
    .toLocaleDateString("en-US", { weekday: "long" })
    .toLowerCase();
  return WEEKDAYS.some((w) => w.key === key) ? key : null;
}

function initialsOf(name: string): string {
  return name
    .split(" ")
    .map((part) => part[0])
    .filter(Boolean)
    .slice(0, 2)
    .join("")
    .toUpperCase();
}

export function EmployeesPage() {
  const { token } = useAuth();
  const [search, setSearch] = useState("");
  const [department, setDepartment] = useState<string | null>(null);
  const [office, setOffice] = useState<string | null>(null);
  const [day, setDay] = useState<string | null>(null);
  const [page, setPage] = useState(0);

  const debouncedSearch = useDebouncedValue(search);
  const todayKey = todayDayKey();

  const { data, isLoading, isError } = useQuery({
    queryKey: ["employees", debouncedSearch, department, office, day, page],
    queryFn: () =>
      searchEmployees(
        {
          search: debouncedSearch || undefined,
          department: department ?? undefined,
          office: office ?? undefined,
          day: day ?? undefined,
          page,
          pageSize: PAGE_SIZE,
        },
        token!
      ),
    enabled: Boolean(token),
  });

  const { data: departmentsPage } = useQuery({
    queryKey: ["departments", "filter-options"],
    queryFn: () => searchDepartments({ page: 0, pageSize: 100 }, token!),
    enabled: Boolean(token),
  });

  function updateFilter(setter: (value: string) => void, value: string) {
    setter(value);
    setPage(0);
  }

  return (
    <div className="space-y-5">
      {/* B-32 (#204): gun secim barı - bir gune tiklayinca liste/rozetler o gunun planina gore hesaplanir. */}
      <div className="inline-flex rounded-lg border p-1">
        {WEEKDAYS.map(({ key, label }) => {
          const isSelected = day === key;
          const isToday = key === todayKey;
          return (
            <button
              key={key}
              type="button"
              onClick={() => {
                setDay((current) => (current === key ? null : key));
                setPage(0);
              }}
              className={`rounded-md px-3 py-1.5 text-sm transition-colors ${
                isSelected
                  ? "bg-muted font-medium"
                  : `text-muted-foreground hover:bg-muted/50 ${isToday ? "ring-primary/40 ring-1" : ""}`
              }`}
            >
              {label}
            </button>
          );
        })}
      </div>

      <div className="grid grid-cols-1 items-end gap-3 sm:grid-cols-3">
        <div className="mb-1.5 space-y-1.5">
          <Label htmlFor="employee-search">Ara</Label>
          <Input
            id="employee-search"
            placeholder="İsimle ara…"
            className="h-[38px]"
            value={search}
            onChange={(event) => updateFilter(setSearch, event.target.value)}
          />
        </div>
        <div className="space-y-1.5">
          <Label>Departman</Label>
          <Select
            value={department}
            onValueChange={(value) => {
              setDepartment(value);
              setPage(0);
            }}
          >
            <SelectTrigger className="h-[38px] w-full">
              <SelectValue placeholder="Tüm Departmanlar" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={null}>Tüm Departmanlar</SelectItem>
              {(departmentsPage?.data ?? []).map((dept) => (
                <SelectItem key={dept.id} value={dept.name}>
                  {dept.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <div className="space-y-1.5">
          <Label>Ofis Durumu</Label>
          <Select
            value={office}
            onValueChange={(value) => {
              setOffice(value);
              setPage(0);
            }}
          >
            <SelectTrigger className="h-[38px] w-full">
              <SelectValue placeholder="Tümü" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={null}>Tümü</SelectItem>
              {OfficeStatusSchema.options.map((status) => (
                <SelectItem key={status} value={status}>
                  {status}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      {isLoading && <p className="text-muted-foreground text-sm">Yükleniyor…</p>}
      {isError && <p className="text-destructive text-sm">Çalışanlar yüklenemedi.</p>}

      {data && (
        <>
          {data.data.length === 0 ? (
            <p className="text-muted-foreground text-sm">Sonuç bulunamadı.</p>
          ) : (
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
              {data.data.map((employee) => (
                <Card key={employee.id}>
                  <CardContent className="flex items-start gap-3">
                    <div className="bg-primary-soft text-primary flex size-11 shrink-0 items-center justify-center rounded-full text-sm font-bold">
                      {initialsOf(employee.name)}
                    </div>
                    <div className="min-w-0 flex-1 space-y-1">
                      <div className="flex items-center justify-between gap-2">
                        <p className="truncate text-sm font-semibold">{employee.name}</p>
                        {/* A-32 (#188): durum bugunun calisma duzeninden geliyor. Plan
                            girilmemisse rozet GIZLENMIYOR — eskiden gizleniyordu ve kullanici
                            "durumu yok" ile "durumu bilinmiyor" arasindaki farki goremiyordu.
                            Bos birakmak, olmayan bilgiyi varmis gibi gostermekten daha az
                            yaniltici degil; acikca soylemek gerekiyor. */}
                        <span
                          className={cn(
                            "shrink-0 rounded-full px-2 py-0.5 text-[11px] font-semibold",
                            employee.officeStatus
                              ? statusStyles[employee.officeStatus as OfficeStatus] ??
                                  "bg-muted text-muted-foreground"
                              : "bg-muted text-muted-foreground italic"
                          )}
                        >
                          {employee.officeStatus ?? "Plan girilmedi"}
                        </span>
                      </div>
                      <p className="text-muted-foreground truncate text-xs">
                        {employee.departmentName ?? "Departman atanmamış"}
                      </p>
                      <p className="text-muted-foreground truncate text-xs">{employee.email}</p>
                      <p className="text-muted-foreground truncate text-xs">
                        {employee.phone ?? "Telefon numarası yok"}
                      </p>
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          )}
          <PaginationControls page={data.page} pageSize={data.pageSize} total={data.total} onPageChange={setPage} />
        </>
      )}
    </div>
  );
}
