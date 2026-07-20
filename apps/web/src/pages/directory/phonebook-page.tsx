import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { PaginationControls } from "@/components/pagination-controls";
import { useDebouncedValue } from "@/hooks/use-debounced-value";
import { useAuth } from "@/auth/auth-context";
import { ApiError } from "@/api/client";
import { searchPhonebook, triggerCall } from "@/api/directory";

const PAGE_SIZE = 15;

export function PhonebookPage() {
  const { token } = useAuth();
  const queryClient = useQueryClient();
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const debouncedSearch = useDebouncedValue(search);

  const { data, isLoading, isError } = useQuery({
    queryKey: ["phonebook", debouncedSearch, page],
    queryFn: () =>
      searchPhonebook({ search: debouncedSearch || undefined, page, pageSize: PAGE_SIZE }, token!),
    enabled: Boolean(token),
  });

  const callMutation = useMutation({
    mutationFn: (extension: string) => triggerCall(extension, token!),
    onSuccess: (result) => {
      toast.success(`${result.extension} aranıyor…`);
      queryClient.invalidateQueries({ queryKey: ["phonebook"] });
    },
    onError: (error: unknown) => {
      const message = error instanceof ApiError ? error.message : "Arama tetiklenemedi";
      toast.error(message);
    },
  });

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold">Telefon Rehberi</h1>

      <div className="max-w-sm space-y-1.5">
        <Label htmlFor="phonebook-search">Ara</Label>
        <Input
          id="phonebook-search"
          placeholder="İsim veya dahili numara…"
          value={search}
          onChange={(event) => {
            setSearch(event.target.value);
            setPage(0);
          }}
        />
      </div>

      {isLoading && <p className="text-muted-foreground text-sm">Yükleniyor…</p>}
      {isError && <p className="text-destructive text-sm">Telefon rehberi yüklenemedi.</p>}

      {data && (
        <>
          {data.data.length === 0 ? (
            <p className="text-muted-foreground text-sm">Sonuç bulunamadı.</p>
          ) : (
            <ul className="divide-y rounded-lg border">
              {data.data.map((entry) => (
                <li key={entry.id} className="flex items-center justify-between gap-3 px-4 py-3">
                  <div>
                    <p className="text-sm font-medium">{entry.name}</p>
                    <p className="text-muted-foreground text-sm">
                      {entry.departmentName ?? "Departman atanmamış"} &middot; Tel: {entry.extension ?? "—"}
                    </p>
                  </div>
                  <Button
                    type="button"
                    size="sm"
                    variant="outline"
                    disabled={!entry.extension || callMutation.isPending}
                    onClick={() => entry.extension && callMutation.mutate(entry.extension)}
                  >
                    Ara
                  </Button>
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
