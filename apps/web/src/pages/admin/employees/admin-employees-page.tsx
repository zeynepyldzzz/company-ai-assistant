import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/auth/auth-context";
import { ApiError } from "@/api/client";
import { searchDepartments, searchEmployees, deleteEmployee } from "@/api/directory";
import { EmployeeFormSheet } from "./employee-form-sheet";

// #84 (Hafta 4): Admin çalışan CRUD ekranı (FR-68-71). Basitlik için sayfalama
// olmadan ilk 100 kayıt listelenir; büyürse /employees endpoint'i zaten
// sayfalıyor, burada da eklenebilir.
export function AdminEmployeesPage() {
  const { token } = useAuth();
  const queryClient = useQueryClient();

  const { data: employeesPage, isLoading, isError } = useQuery({
    queryKey: ["employees", "admin-list"],
    queryFn: () => searchEmployees({ page: 0, pageSize: 100 }, token!),
    enabled: Boolean(token),
  });

  const { data: departmentsPage } = useQuery({
    queryKey: ["departments", "admin-list"],
    queryFn: () => searchDepartments({ page: 0, pageSize: 100 }, token!),
    enabled: Boolean(token),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deleteEmployee(id, token!),
    onSuccess: () => {
      toast.success("Çalışan silindi.");
      queryClient.invalidateQueries({ queryKey: ["employees"] });
    },
    onError: (error) => {
      const message = error instanceof ApiError ? error.message : "Çalışan silinemedi.";
      toast.error(message);
    },
  });

  const departments = departmentsPage?.data ?? [];

  return (
    <div className="space-y-4">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-xl font-semibold">Çalışan Yönetimi</h1>
          <p className="text-muted-foreground text-sm">Çalışan oluşturun, düzenleyin, silin.</p>
        </div>
        <EmployeeFormSheet departments={departments} />
      </div>

      {isLoading && <p className="text-muted-foreground text-sm">Yükleniyor…</p>}
      {isError && <p className="text-destructive text-sm">Çalışanlar yüklenemedi.</p>}

      {employeesPage && (
        <div className="overflow-x-auto rounded-lg border">
          <table className="w-full text-sm">
            <thead className="bg-muted/50">
              <tr>
                <th className="px-4 py-2 text-left font-medium">İsim</th>
                <th className="px-4 py-2 text-left font-medium">E-posta</th>
                <th className="px-4 py-2 text-left font-medium">Departman</th>
                <th className="px-4 py-2 text-right font-medium">İşlemler</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {employeesPage.data.length === 0 ? (
                <tr>
                  <td colSpan={4} className="text-muted-foreground px-4 py-3 text-center">
                    Henüz çalışan yok.
                  </td>
                </tr>
              ) : (
                employeesPage.data.map((employee) => (
                  <tr key={employee.id}>
                    <td className="px-4 py-2 font-medium">{employee.name}</td>
                    <td className="text-muted-foreground px-4 py-2">{employee.email}</td>
                    <td className="text-muted-foreground px-4 py-2">
                      {employee.departmentName ?? "Atanmamış"}
                    </td>
                    <td className="px-4 py-2">
                      <div className="flex justify-end gap-2">
                        <EmployeeFormSheet employee={employee} departments={departments} />
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => {
                            if (confirm(`${employee.name} silinsin mi?`)) {
                              deleteMutation.mutate(employee.id);
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
