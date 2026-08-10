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
  const [firstName, setFirstName] = useState(employee?.firstName ?? "");
  const [lastName, setLastName] = useState(employee?.lastName ?? "");
  const [email, setEmail] = useState(employee?.email ?? "");
  const [phone, setPhone] = useState(employee?.phone ?? "");
  const [departmentId, setDepartmentId] = useState<string | null>(
    employee?.departmentId ? String(employee.departmentId) : null
  );
  // C-12 (#120): olusturmada ilk sifre zorunlu; duzenlemede bos birakilirsa
  // mevcut sifre degismez (bkz. AdminEmployeeService.applyRequest).
  // A-29: sistem uretirse gecici sifre burada tutulur ve panelde BIR KEZ gosterilir.
  const [generatedPassword, setGeneratedPassword] = useState<string | null>(null);

  useEffect(() => {
    if (!open) return;
    setFirstName(employee?.firstName ?? "");
    setLastName(employee?.lastName ?? "");
    setEmail(employee?.email ?? "");
    setPhone(employee?.phone ?? "");
    setDepartmentId(employee?.departmentId ? String(employee.departmentId) : null);
    setGeneratedPassword(null);
  }, [open, employee]);

  const mutation = useMutation({
    // A-29: olusturma ve guncelleme FARKLI tip donuyor (olusturmada uretilen sifre var).
    // Donus tipi acikca yazilmazsa TypeScript union'i cikaramiyor.
    mutationFn: (): Promise<Employee | CreateEmployeeResponse> => {
      const body: AdminEmployeeRequest = {
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        email: email.trim(),
        phone: phone.trim() || null,
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
    // A-35 (#196): soyad da zorunlu. Backend @NotBlank ile reddediyor; buradaki kontrol
    // istegi bosa gondermemek icin. Eski kayitlarda soyad NULL olabilir, o yuzden
    // duzenleme akisinda bos gelen bir soyadin doldurulmasi gerekir.
    if (!firstName.trim() || !lastName.trim() || !email.trim()) {
      toast.error("Ad, soyad ve e-posta boş olamaz.");
      return;
    }
    // A-30 (#185): departman zorunlu. Backend de @NotNull ile reddediyor; buradaki kontrol
    // istegi bosa gondermemek icin.
    if (!departmentId) {
      toast.error("Departman seçilmelidir.");
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
          {/* A-35 (#196): ad ve soyad ayri alanlar. Tek "İsim" alani oldugu icin rehber ada
              gore siralaniyordu ve arama "kelime basi"ni bosluk karakteriyle taklit etmek
              zorundaydi. */}
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <Label htmlFor="employee-first-name">Ad</Label>
              <Input
                id="employee-first-name"
                value={firstName}
                onChange={(event) => setFirstName(event.target.value)}
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="employee-last-name">Soyad</Label>
              <Input
                id="employee-last-name"
                value={lastName}
                onChange={(event) => setLastName(event.target.value)}
              />
            </div>
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
            {/* Harf girisi engellenir ama bicim KATI DEGIL: bu alan hem cep numarasi
                (0532 111 22 33) hem 4 haneli dahili tutuyor. Sabit bir maske dahiliyi
                girilemez yapardi. */}
            <Input
              id="employee-phone"
              inputMode="tel"
              maxLength={20}
              placeholder="0532 111 22 33 veya 1005"
              value={phone ?? ""}
              onChange={(event) => setPhone(event.target.value.replace(/[^\d\s()+-]/g, ""))}
            />
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

          {/* A-32 (#188): "Ofis Durumu" alani KALDIRILDI. Durum artik bugunun calisma
              duzeninden turetiliyor, dolayisiyla buradan girilen deger hicbir yerde
              gorunmezdi — admin veriyi girdigini sanir, sistem gormezden gelirdi. */}

          <div className="space-y-1.5">
            <Label>Departman *</Label>
            <Select value={departmentId} onValueChange={setDepartmentId}>
              <SelectTrigger>
                {/* Base UI Select secili ogenin ETIKETINI kendiliginden bulmuyor; deger ile
                    etiket farkli oldugunda (burada deger id, etiket ad) ham degeri basiyor
                    ve ekranda "3" gorunuyordu. Ofis durumu acilirinda sorun cikmiyor cunku
                    orada deger ve etiket ayni. Cozum admin-roles-page'de kullanilan desen.
                    A-30: bos halin metni artik "Atanmamis" degil — departman zorunlu oldugu
                    icin bos hal bir DURUM degil, yapilmamis bir SECIM. */}
                <SelectValue placeholder="Departman seçin…">
                  {(value: string | null) =>
                    departments.find((department) => String(department.id) === value)?.name ??
                    "Departman seçin…"
                  }
                </SelectValue>
              </SelectTrigger>
              <SelectContent>
                {/* A-30: "Atanmamış" secenegi KALDIRILDI — departmansiz calisan uretmenin yolu
                    kapandi. Mevcut departmansiz kayitlar duzenlenirken de secim zorunlu olur. */}
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
