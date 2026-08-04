import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/auth/auth-context";
import { ApiError } from "@/api/client";
import {
  searchDepartments,
  searchEmployees,
  deleteEmployee,
  resetEmployeePassword,
} from "@/api/directory";
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

  // A-29: sifre sifirlama. Uretilen gecici sifre BIR KEZ donuyor ve burada gosteriliyor;
  // toast yerine kalici bir kutu, cunku sifre kayboldugunda tekrar okunamaz.
  const [resetResult, setResetResult] = useState<{ name: string; password: string } | null>(null);

  const resetPasswordMutation = useMutation({
    mutationFn: (id: number) => resetEmployeePassword(id, token!),
    onSuccess: (result) => {
      if (result.generatedPassword) {
        setResetResult({ name: result.employee.name, password: result.generatedPassword });
      }
    },
    onError: (error) => {
      const message = error instanceof ApiError ? error.message : "Şifre sıfırlanamadı.";
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

      {/* A-29: uretilen gecici sifre burada gosterilir. Kapatilana kadar durur — kaybolursa
          tekrar okunamaz, yalnizca yeniden sifirlanabilir. */}
      {resetResult && (
        <div className="bg-muted flex flex-wrap items-center justify-between gap-3 rounded-lg border px-4 py-3">
          <div className="space-y-1">
            <p className="text-sm">
              <span className="font-medium">{resetResult.name}</span> için geçici şifre:{" "}
              <code className="font-mono">{resetResult.password}</code>
            </p>
            <p className="text-muted-foreground text-xs">
              Çalışana iletin; ilk girişinde kendi şifresini belirleyecek. Bu şifre bir daha
              gösterilmeyecek.
            </p>
          </div>
          <div className="flex gap-2">
            <Button
              size="sm"
              variant="outline"
              onClick={() => {
                navigator.clipboard.writeText(resetResult.password);
                toast.success("Şifre kopyalandı.");
              }}
            >
              Kopyala
            </Button>
            <Button size="sm" onClick={() => setResetResult(null)}>
              Tamam
            </Button>
          </div>
        </div>
      )}

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
                            if (
                              confirm(
                                `${employee.name} için yeni bir geçici şifre üretilsin mi? Mevcut şifresi geçersiz olacak.`
                              )
                            ) {
                              resetPasswordMutation.mutate(employee.id);
                            }
                          }}
                          disabled={resetPasswordMutation.isPending}
                        >
                          Şifre sıfırla
                        </Button>
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
