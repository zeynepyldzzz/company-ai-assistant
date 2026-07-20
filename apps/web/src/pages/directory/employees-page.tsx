import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { OfficeStatusSchema } from "@company/shared";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { PaginationControls } from "@/components/pagination-controls";
import { useDebouncedValue } from "@/hooks/use-debounced-value";
import { useAuth } from "@/auth/auth-context";
import { searchEmployees } from "@/api/directory";

const PAGE_SIZE = 12;

export function EmployeesPage() {
  const { token } = useAuth();
  const [search, setSearch] = useState("");
  const [department, setDepartment] = useState("");
  const [office, setOffice] = useState<string | null>(null);
  const [page, setPage] = useState(0);

  const debouncedSearch = useDebouncedValue(search);
  const debouncedDepartment = useDebouncedValue(department);

  const { data, isLoading, isError } = useQuery({
    queryKey: ["employees", debouncedSearch, debouncedDepartment, office, page],
    queryFn: () =>
      searchEmployees(
        {
          search: debouncedSearch || undefined,
          department: debouncedDepartment || undefined,
          office: office ?? undefined,
          page,
          pageSize: PAGE_SIZE,
        },
        token!
      ),
    enabled: Boolean(token),
  });

  function updateFilter(setter: (value: string) => void, value: string) {
    setter(value);
    setPage(0);
  }

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold">Çalışan Rehberi</h1>

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
        <div className="space-y-1.5">
          <Label htmlFor="employee-search">Ara</Label>
          <Input
            id="employee-search"
            placeholder="İsimle ara…"
            value={search}
            onChange={(event) => updateFilter(setSearch, event.target.value)}
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="employee-department">Departman</Label>
          <Input
            id="employee-department"
            placeholder="Departman adı…"
            value={department}
            onChange={(event) => updateFilter(setDepartment, event.target.value)}
          />
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
            <SelectTrigger>
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
                  <CardHeader>
                    <CardTitle>{employee.name}</CardTitle>
                    <CardDescription>{employee.departmentName ?? "Departman atanmamış"}</CardDescription>
                  </CardHeader>
                  <CardContent>
                    <p>Ofis durumu: {employee.officeStatus ?? "Belirtilmemiş"}</p>
                    <p>Telefon: {employee.phone ?? "—"}</p>
                    <p>E-posta: {employee.email}</p>
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
