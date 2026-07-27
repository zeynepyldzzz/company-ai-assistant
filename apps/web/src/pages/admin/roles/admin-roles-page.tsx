import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { useAuth } from "@/auth/auth-context";
import { ApiError } from "@/api/client";
import { searchEmployees } from "@/api/directory";
import { listRoles, updateUserRole } from "@/api/roles";

// C-11 (#85): Rol/Izin yonetim ekrani - yalnizca system_admin (FR-80-82).
// Her calisan satirinda mevcut rolu gosterir (EmployeeResponse.roleId), yeni
// rol secilince PUT /admin/users/{id}/roles cagrilir.
export function AdminRolesPage() {
  const { token } = useAuth();
  const queryClient = useQueryClient();

  const { data: employeesPage, isLoading, isError } = useQuery({
    queryKey: ["employees", "admin-list"],
    queryFn: () => searchEmployees({ page: 0, pageSize: 100 }, token!),
    enabled: Boolean(token),
  });

  const { data: roles } = useQuery({
    queryKey: ["roles"],
    queryFn: () => listRoles(token!),
    enabled: Boolean(token),
  });

  const mutation = useMutation({
    mutationFn: ({ employeeId, roleId }: { employeeId: number; roleId: number }) =>
      updateUserRole(employeeId, { roleId }, token!),
    onSuccess: (result) => {
      toast.success(`${result.employeeName} icin rol "${result.roleName}" olarak guncellendi.`);
      queryClient.invalidateQueries({ queryKey: ["employees"] });
    },
    onError: (error) => {
      const message = error instanceof ApiError ? error.message : "Rol guncellenemedi.";
      toast.error(message);
    },
  });

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-xl font-semibold">Rol / Izin Yonetimi</h1>
        <p className="text-muted-foreground text-sm">
          Kullanicilara rol atayin veya kaldirin. Yalnizca system_admin erisebilir.
        </p>
      </div>

      {isLoading && <p className="text-muted-foreground text-sm">Yükleniyor…</p>}
      {isError && <p className="text-destructive text-sm">Çalışanlar yüklenemedi.</p>}

      {employeesPage && roles && (
        <div className="overflow-x-auto rounded-lg border">
          <table className="w-full text-sm">
            <thead className="bg-muted/50">
              <tr>
                <th className="px-4 py-2 text-left font-medium">İsim</th>
                <th className="px-4 py-2 text-left font-medium">E-posta</th>
                <th className="px-4 py-2 text-left font-medium">Rol</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {employeesPage.data.map((employee) => (
                <tr key={employee.id}>
                  <td className="px-4 py-2 font-medium">{employee.name}</td>
                  <td className="text-muted-foreground px-4 py-2">{employee.email}</td>
                  <td className="px-4 py-2">
                    <Select
                      value={employee.roleId ? String(employee.roleId) : null}
                      onValueChange={(value) => {
                        if (!value) return;
                        mutation.mutate({ employeeId: employee.id, roleId: Number(value) });
                      }}
                    >
                      <SelectTrigger className="w-48">
                        <SelectValue placeholder="Rol seç...">
                          {(value: string | null) =>
                            roles.find((role) => String(role.id) === value)?.name ?? "Rol seç..."
                          }
                        </SelectValue>
                      </SelectTrigger>
                      <SelectContent>
                        {roles.map((role) => (
                          <SelectItem key={role.id} value={String(role.id)}>
                            {role.name}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
