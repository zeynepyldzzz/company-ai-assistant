import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useNavigate } from "react-router";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent } from "@/components/ui/card";
import { PaginationControls } from "@/components/pagination-controls";
import { useDebouncedValue } from "@/hooks/use-debounced-value";
import { useAuth } from "@/auth/auth-context";
import { searchDepartments } from "@/api/directory";

const PAGE_SIZE = 10;

export function DepartmentsPage() {
  const { token } = useAuth();
  const navigate = useNavigate();
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const debouncedSearch = useDebouncedValue(search);

  const { data, isLoading, isError } = useQuery({
    queryKey: ["departments", debouncedSearch, page],
    queryFn: () =>
      searchDepartments({ search: debouncedSearch || undefined, page, pageSize: PAGE_SIZE }, token!),
    enabled: Boolean(token),
  });

  return (
    <div className="space-y-5">
      <h1 className="text-[22px] font-extrabold">Departmanlar</h1>

      <div className="max-w-sm space-y-1.5">
        <Label htmlFor="department-search">Ara</Label>
        <Input
          id="department-search"
          placeholder="Departman adıyla ara…"
          className="h-[38px]"
          value={search}
          onChange={(event) => {
            setSearch(event.target.value);
            setPage(0);
          }}
        />
      </div>

      {isLoading && <p className="text-muted-foreground text-sm">Yükleniyor…</p>}
      {isError && <p className="text-destructive text-sm">Departmanlar yüklenemedi.</p>}

      {data && (
        <>
          {data.data.length === 0 ? (
            <p className="text-muted-foreground text-sm">Sonuç bulunamadı.</p>
          ) : (
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              {data.data.map((department) => (
                <Card
                  key={department.id}
                  className="hover:border-primary/40 cursor-pointer transition-colors"
                  onClick={() => navigate(`/directory/departments/${department.id}`)}
                >
                  <CardContent className="space-y-1.5">
                    <p className="text-[15px] font-semibold">{department.name}</p>
                    {department.responsibilities && (
                      <p className="text-muted-foreground line-clamp-2 text-sm">
                        {department.responsibilities}
                      </p>
                    )}
                    <p className="text-muted-foreground text-xs">
                      Sorumlu: {department.managerName ?? "Atanmamış"}
                    </p>
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
