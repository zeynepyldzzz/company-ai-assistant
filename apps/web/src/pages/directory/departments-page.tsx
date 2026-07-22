import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useNavigate } from "react-router";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
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
    <div className="space-y-4">
      <h1 className="text-xl font-semibold">Departmanlar</h1>

      <div className="max-w-sm space-y-1.5">
        <Label htmlFor="department-search">Ara</Label>
        <Input
          id="department-search"
          placeholder="Departman adıyla ara…"
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
            <ul className="divide-y rounded-lg border">
              {data.data.map((department) => (
                <li key={department.id}>
                  <button
                    type="button"
                    onClick={() => navigate(`/directory/departments/${department.id}`)}
                    className="hover:bg-muted flex w-full flex-col items-start gap-0.5 px-4 py-3 text-left transition-colors"
                  >
                    <span className="text-sm font-medium">{department.name}</span>
                    {department.responsibilities && (
                      <span className="text-muted-foreground line-clamp-1 text-sm">
                        {department.responsibilities}
                      </span>
                    )}
                  </button>
                </li>
              ))}
            </ul>
          )}
          <PaginationControls page={data.page} pageSize={data.pageSize} total={data.total} onPageChange={setPage} />
        </>
      )}
    </div>
  );
}
