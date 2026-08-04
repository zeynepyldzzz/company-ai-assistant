import { useEffect, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Plus, Pencil } from "lucide-react";
import type {
  AdminEmployeeRequest,
  CreateEmployeeResponse,
  Department,
  Employee,
} from "@company/shared";
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
import { createEmployee, updateEmployee } from "@/api/directory";

// #84 (Hafta 4): tek form hem POST hem PUT icin kullanilir (employee prop'u
// verilmisse duzenleme, verilmemisse olusturma modu).
export function EmployeeFormSheet({
  employee,
  departments,
}: {
  employee?: Employee;
  departments: Department[];
}) {
  const { token } = useAuth();
  const queryClient = useQueryClient();
  const isEdit = Boolean(employee);

  const [open, setOpen] = useState(false);
  const [name, setName] = useState(employee?.name ?? "");
  const [email, setEmail] = useState(employee?.email ?? "");
  const [phone, setPhone] = useState(employee?.phone ?? "");
  const [officeStatus, setOfficeStatus] = useState<string | null>(employee?.officeStatus ?? null);
  const [departmentId, setDepartmentId] = useState<string | null>(
    employee?.departmentId ? String(employee.departmentId) : null
  );
  // C-12 (#120): olusturmada ilk sifre zorunlu; duzenlemede bos birakilirsa
  // mevcut sifre degismez (bkz. AdminEmployeeService.applyRequest).
  // A-29: sistem uretirse gecici sifre burada tutulur ve panelde BIR KEZ gosterilir.
  const [generatedPassword, setGeneratedPassword] = useState<string | null>(null);

  useEffect(() => {
    if (!open) return;
    setName(employee?.name ?? "");
    setEmail(employee?.email ?? "");
    setPhone(employee?.phone ?? "");
    setOfficeStatus(employee?.officeStatus ?? null);
    setDepartmentId(employee?.departmentId ? String(employee.departmentId) : null);
    setGeneratedPassword(null);
  }, [open, employee]);

  const mutation = useMutation({
    // A-29: olusturma ve guncelleme FARKLI tip donuyor (olusturmada uretilen sifre var).
    // Donus tipi acikca yazilmazsa TypeScript union'i cikaramiyor.
    mutationFn: (): Promise<Employee | CreateEmployeeResponse> => {
      const body: AdminEmployeeRequest = {
        name: name.trim(),
        email: email.trim(),
        phone: phone.trim() || null,
        officeStatus: officeStatus,
        departmentId: departmentId ? Number(departmentId) : null,
      };
      return isEdit ? updateEmployee(employee!.id, body, token!) : createEmployee(body, token!);
    },
    onSuccess: (result) => {
      queryClient.invalidateQueries({ queryKey: ["employees"] });
      toast.success(isEdit ? "Çalışan güncellendi." : "Çalışan oluşturuldu.");

      // A-29: sistem sifre urettiyse panel ACIK kalir ve sifre bir kez gosterilir.
      // Kapatirsak sifre kaybolur — veritabaninda yalnizca hash var, tekrar okunamaz.
      if ("generatedPassword" in result && result.generatedPassword) {
        setGeneratedPassword(result.generatedPassword);
        return;
      }
      setOpen(false);
    },
    onError: (error) => {
      const message = error instanceof ApiError ? error.message : "İşlem başarısız oldu.";
      toast.error(message);
    },
  });

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    if (!name.trim() || !email.trim()) {
      toast.error("İsim ve e-posta boş olamaz.");
      return;
    }
    // A-29: sifre artik zorunlu degil — bos birakilirsa sistem gecici sifre uretir.
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
              Yeni Çalışan
            </Button>
          )
        }
      />
      <SheetContent className="w-full overflow-y-auto sm:max-w-lg">
        <SheetHeader>
          <SheetTitle>{isEdit ? "Çalışanı Düzenle" : "Yeni Çalışan"}</SheetTitle>
          <SheetDescription>Yalnızca hr_admin / system_admin erişebilir (FR-68-71).</SheetDescription>
        </SheetHeader>

        {/* A-29: sistem sifre urettiginde form yerine bu panel gosterilir. Sifre yalnizca
            burada okunabilir; veritabaninda hash tutuldugu icin bir daha gorulemez. */}
        {generatedPassword ? (
          <div className="flex flex-1 flex-col gap-4 px-4">
            <p className="text-sm">
              Çalışan oluşturuldu. Aşağıdaki geçici şifreyi çalışana iletin — ilk girişinde kendi
              şifresini belirlemesi istenecek.
            </p>
            <div className="bg-muted flex items-center justify-between gap-2 rounded-md px-3 py-2">
              <code className="font-mono text-sm break-all">{generatedPassword}</code>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => {
                  navigator.clipboard.writeText(generatedPassword);
                  toast.success("Şifre kopyalandı.");
                }}
              >
                Kopyala
              </Button>
            </div>
            <p className="text-warning text-xs">
              Bu şifre bir daha gösterilmeyecek. Kaybederseniz çalışana yeni bir şifre
              belirlemeniz gerekir.
            </p>
            <div className="mt-auto pb-4">
              <Button type="button" onClick={() => setOpen(false)}>
                Tamam
              </Button>
            </div>
          </div>
        ) : (
        <form onSubmit={handleSubmit} className="flex flex-1 flex-col gap-4 px-4">
          <div className="space-y-1.5">
            <Label htmlFor="employee-name">İsim</Label>
            <Input id="employee-name" value={name} onChange={(event) => setName(event.target.value)} />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="employee-email">E-posta</Label>
            <Input
              id="employee-email"
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="employee-phone">Telefon</Label>
            <Input id="employee-phone" value={phone ?? ""} onChange={(event) => setPhone(event.target.value)} />
          </div>

          {/* A-29: sifre alani KALDIRILDI — admin hicbir yolla sifre belirlemiyor.
              Olusturmada sistem gecici sifre uretir, sifirlama listedeki aksiyondan yapilir. */}
          {!isEdit && (
            <p className="text-muted-foreground text-xs">
              Kaydettiğinde sistem geçici bir şifre üretecek ve sana bir kez gösterecek. Çalışan
              ilk girişinde kendi şifresini belirler. Admin türü bir rol verirsen girişte iki
              faktörlü doğrulama (QR kod ile) da istenecektir.
            </p>
          )}

          <div className="space-y-1.5">
            <Label>Ofis Durumu</Label>
            <Select value={officeStatus} onValueChange={setOfficeStatus}>
              <SelectTrigger>
                <SelectValue placeholder="Belirtilmemiş" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={null}>Belirtilmemiş</SelectItem>
                <SelectItem value="Ofiste">Ofiste</SelectItem>
                <SelectItem value="Uzaktan">Uzaktan</SelectItem>
                <SelectItem value="Izinde">İzinde</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-1.5">
            <Label>Departman</Label>
            <Select value={departmentId} onValueChange={setDepartmentId}>
              <SelectTrigger>
                <SelectValue placeholder="Atanmamış" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={null}>Atanmamış</SelectItem>
                {departments.map((department) => (
                  <SelectItem key={department.id} value={String(department.id)}>
                    {department.name}
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
        )}
      </SheetContent>
    </Sheet>
  );
}
