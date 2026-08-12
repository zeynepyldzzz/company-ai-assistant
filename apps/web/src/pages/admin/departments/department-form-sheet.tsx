import { useEffect, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Plus, Pencil } from "lucide-react";
import type { AdminDepartmentRequest, Department, Employee } from "@company/shared";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetFooter,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";
import { useAuth } from "@/auth/auth-context";
import { ApiError } from "@/api/client";
import { createDepartment, updateDepartment } from "@/api/directory";

// #84 (Hafta 4): tek form hem POST hem PUT icin (department prop'u varsa duzenleme).
export function DepartmentFormSheet({
  department,
  employees,
}: {
  department?: Department;
  employees: Employee[];
}) {
  const { token } = useAuth();
  const queryClient = useQueryClient();
  const isEdit = Boolean(department);

  const [open, setOpen] = useState(false);
  const [name, setName] = useState(department?.name ?? "");
  const [responsibilities, setResponsibilities] = useState(department?.responsibilities ?? "");
  const [managerId, setManagerId] = useState<string | null>(
    department?.managerId ? String(department.managerId) : null
  );

  useEffect(() => {
    if (!open) return;
    setName(department?.name ?? "");
    setResponsibilities(department?.responsibilities ?? "");
    setManagerId(department?.managerId ? String(department.managerId) : null);
  }, [open, department]);

  const mutation = useMutation({
    mutationFn: () => {
      const body: AdminDepartmentRequest = {
        name: name.trim(),
        responsibilities: responsibilities.trim() || null,
        managerId: managerId ? Number(managerId) : null,
      };
      return isEdit ? updateDepartment(department!.id, body, token!) : createDepartment(body, token!);
    },
    onSuccess: () => {
      toast.success(isEdit ? "Departman güncellendi." : "Departman oluşturuldu.");
      queryClient.invalidateQueries({ queryKey: ["departments"] });
      setOpen(false);
    },
    onError: (error) => {
      const message = error instanceof ApiError ? error.message : "İşlem başarısız oldu.";
      toast.error(message);
    },
  });

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    if (!name.trim()) {
      toast.error("Departman adı boş olamaz.");
      return;
    }
    mutation.mutate();
  }

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      <SheetTrigger
        render={
          isEdit ? (
            <Button variant="outline" size="sm">
              <Pencil />
              Düzenle
            </Button>
          ) : (
            <Button>
              <Plus />
              Yeni Departman
            </Button>
          )
        }
      />
      <SheetContent className="w-full overflow-y-auto sm:max-w-lg">
        <SheetHeader>
          <SheetTitle>{isEdit ? "Departmanı Düzenle" : "Yeni Departman"}</SheetTitle>
          <SheetDescription>
            Bu ekrana yalnızca İK Yöneticisi ve Sistem Yöneticisi erişebilir.
          </SheetDescription>
        </SheetHeader>

        <form onSubmit={handleSubmit} className="flex flex-1 flex-col gap-4 px-4">
          <div className="space-y-1.5">
            <Label htmlFor="department-name">Ad</Label>
            <Input id="department-name" value={name} onChange={(event) => setName(event.target.value)} />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="department-resp">Sorumluluklar</Label>
            <Input
              id="department-resp"
              value={responsibilities ?? ""}
              onChange={(event) => setResponsibilities(event.target.value)}
            />
          </div>

          <div className="space-y-1.5">
            <Label>Yönetici</Label>
            <Select value={managerId} onValueChange={setManagerId}>
              <SelectTrigger>
                {/* Deger calisan id oldugu icin secim sonrasi ham id gorunuyordu. */}
                <SelectValue placeholder="Atanmamış">
                  {(value: string | null) =>
                    employees.find((employee) => String(employee.id) === value)?.name ??
                    "Atanmamış"
                  }
                </SelectValue>
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={null}>Atanmamış</SelectItem>
                {employees.map((employee) => (
                  <SelectItem key={employee.id} value={String(employee.id)}>
                    {employee.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <SheetFooter className="px-0">
            <Button type="submit" disabled={mutation.isPending}>
              {mutation.isPending ? "Kaydediliyor…" : isEdit ? "Güncelle" : "Oluştur"}
            </Button>
          </SheetFooter>
        </form>
      </SheetContent>
    </Sheet>
  );
}
