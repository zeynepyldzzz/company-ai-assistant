import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/auth/auth-context";
import { ApiError } from "@/api/client";
import { searchDepartments, searchEmployees, deleteDepartment } from "@/api/directory";
import { DepartmentFormSheet } from "./department-form-sheet";

// #84 (Hafta 4): Admin departman CRUD ekranı (FR-68-71).
export function AdminDepartmentsPage() {
  const { token } = useAuth();
  const queryClient = useQueryClient();

  const { data: departmentsPage, isLoading, isError } = useQuery({
    queryKey: ["departments", "admin-list"],
    queryFn: () => searchDepartments({ page: 0, pageSize: 100 }, token!),
    enabled: Boolean(token),
  });

  const { data: employeesPage } = useQuery({
    queryKey: ["employees", "admin-list"],
    queryFn: () => searchEmployees({ page: 0, pageSize: 100 }, token!),
    enabled: Boolean(token),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deleteDepartment(id, token!),
    onSuccess: () => {
      toast.success("Departman silindi.");
      queryClient.invalidateQueries({ queryKey: ["departments"] });
    },
    onError: (error) => {
      // FR-71: calisani olan departman silinemez -> backend 409 doner.
      const message = error instanceof ApiError ? error.message : "Departman silinemedi.";
      toast.error(message);
    },
  });

  const employees = employeesPage?.data ?? [];

  return (
    <div className="space-y-4">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-xl font-semibold">Departman Yönetimi</h1>
          <p className="text-muted-foreground text-sm">Departman oluşturun, düzenleyin, silin.</p>
        </div>
        <DepartmentFormSheet employees={employees} />
      </div>

      {isLoading && <p className="text-muted-foreground text-sm">Yükleniyor…</p>}
      {isError && <p className="text-destructive text-sm">Departmanlar yüklenemedi.</p>}

      {departmentsPage && (
        <div className="overflow-x-auto rounded-lg border">
          <table className="w-full text-sm">
            <thead className="bg-muted/50">
              <tr>
                <th className="px-4 py-2 text-left font-medium">Ad</th>
                <th className="px-4 py-2 text-left font-medium">Yönetici</th>
                <th className="px-4 py-2 text-right font-medium">İşlemler</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {departmentsPage.data.length === 0 ? (
                <tr>
                  <td colSpan={3} className="text-muted-foreground px-4 py-3 text-center">
                    Henüz departman yok.
                  </td>
                </tr>
              ) : (
                departmentsPage.data.map((department) => (
                  <tr key={department.id}>
                    <td className="px-4 py-2 font-medium">{department.name}</td>
                    <td className="text-muted-foreground px-4 py-2">
                      {department.managerName ?? "Atanmamış"}
                    </td>
                    <td className="px-4 py-2">
                      <div className="flex justify-end gap-2">
                        <DepartmentFormSheet department={department} employees={employees} />
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => {
                            if (confirm(`${department.name} silinsin mi?`)) {
                              deleteMutation.mutate(department.id);
                            }
                          }}
                          disabled={deleteMutation.isPending}
                        >
                          <Trash2 />
                          Sil
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
