import { useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Upload, Trash2, Check } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/auth/auth-context";
import { ApiError } from "@/api/client";
import { importMenuExcel, deleteMenu, getWeeklyMenu } from "@/api/menu";

// Excel'deki kategori kodlari (backend enum adiyla ayni) -> ekranda gosterilecek Turkce ad.
const CATEGORY_LABELS: Record<string, string> = {
  CORBA: "Çorba",
  ANA_YEMEK: "Ana Yemek",
  PILAV_MAKARNA: "Pilav / Makarna",
  TATLI_ICECEK: "Tatlı / İçecek",
  MEYVE: "Meyve",
  SALATA: "Salata",
  ZEYTINYAGLI_SEBZE: "Zeytinyağlı Sebze",
  YARDIMCI_SALATA: "Yardımcı Salata / Turşu",
  YOGURT_CACIK: "Yoğurt / Cacık",
};

function formatDate(isoDate: string): string {
  return new Date(isoDate).toLocaleDateString("tr-TR", {
    weekday: "long",
    day: "numeric",
    month: "long",
  });
}

// C-2 (#18): admin excel yukleyip once ONIZLEME (commit=false) gorur, onaylarsa
// commit=true ile veritabanina yazilir. Ayni ucu iki asamali cagirmak, yanlislikla
// bozuk bir excel'in dogrudan uretim verisine yazilmasini onler.
export function AdminMenuPage() {
  const { token } = useAuth();
  const queryClient = useQueryClient();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  const { data: weeklyMenu, isLoading: weeklyLoading } = useQuery({
    queryKey: ["menu", "weekly"],
    queryFn: () => getWeeklyMenu(token!),
    enabled: Boolean(token),
  });

  const previewMutation = useMutation({
    mutationFn: (file: File) => importMenuExcel(file, false, token!),
    onError: (error) => {
      const message = error instanceof ApiError ? error.message : "Dosya ayrıştırılamadı.";
      toast.error(message);
    },
  });

  const commitMutation = useMutation({
    mutationFn: (file: File) => importMenuExcel(file, true, token!),
    onSuccess: (result) => {
      toast.success(`${result.daysFound} günlük menü kaydedildi.`);
      queryClient.invalidateQueries({ queryKey: ["menu"] });
      previewMutation.reset();
      setSelectedFile(null);
      if (fileInputRef.current) fileInputRef.current.value = "";
    },
    onError: (error) => {
      const message = error instanceof ApiError ? error.message : "Menü kaydedilemedi.";
      toast.error(message);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deleteMenu(id, token!),
    onSuccess: () => {
      toast.success("Menü silindi.");
      queryClient.invalidateQueries({ queryKey: ["menu"] });
    },
    onError: () => {
      toast.error("Menü silinemedi.");
    },
  });

  function handleFileChange(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0] ?? null;
    setSelectedFile(file);
    previewMutation.reset();
    if (file) {
      previewMutation.mutate(file);
    }
  }

  const preview = previewMutation.data;

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-xl font-semibold">Menü Yönetimi</h1>
        <p className="text-muted-foreground text-sm">
          Yemekhaneden gelen Excel şablonunu yükleyin, önizlemeyi kontrol edip kaydedin.
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Excel Yükle</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex items-center gap-3">
            <Button type="button" variant="outline" onClick={() => fileInputRef.current?.click()}>
              <Upload />
              Dosya Seç
            </Button>
            <span className="text-muted-foreground text-sm">
              {selectedFile ? selectedFile.name : "Henüz dosya seçilmedi (.xlsx)"}
            </span>
            <input
              ref={fileInputRef}
              type="file"
              accept=".xlsx"
              className="hidden"
              onChange={handleFileChange}
            />
          </div>

          {previewMutation.isPending && (
            <p className="text-muted-foreground text-sm">Dosya ayrıştırılıyor…</p>
          )}

          {preview && (
            <div className="space-y-3">
              <div className="text-sm">
                <span className="font-medium">{preview.daysFound}</span> gün bulundu
                {preview.daysWithNoData > 0 && (
                  <span className="text-muted-foreground">
                    {" "}
                    ({preview.daysWithNoData} günde veri yok)
                  </span>
                )}
                {" — bu bir önizleme, henüz kaydedilmedi."}
              </div>

              {preview.warnings.length > 0 && (
                <ul className="border-destructive/30 bg-destructive/5 text-destructive space-y-1 rounded-md border p-3 text-xs">
                  {preview.warnings.map((warning, index) => (
                    <li key={index}>{warning}</li>
                  ))}
                </ul>
              )}

              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
                {preview.days.map((day) => (
                  <div key={day.date} className="rounded-lg border p-3">
                    <p className="text-sm font-medium">{formatDate(day.date)}</p>
                    {day.items.length === 0 ? (
                      <p className="text-muted-foreground mt-1 text-xs">Veri yok</p>
                    ) : (
                      <ul className="mt-1 space-y-0.5 text-xs">
                        {day.items.map((item, index) => (
                          <li key={index}>
                            <span className="text-muted-foreground">
                              {CATEGORY_LABELS[item.category] ?? item.category}:
                            </span>{" "}
                            {item.name}
                          </li>
                        ))}
                      </ul>
                    )}
                  </div>
                ))}
              </div>

              <Button
                type="button"
                onClick={() => selectedFile && commitMutation.mutate(selectedFile)}
                disabled={commitMutation.isPending}
              >
                <Check />
                Onayla ve Kaydet
              </Button>
            </div>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Bu Haftaki Menüler</CardTitle>
        </CardHeader>
        <CardContent className="space-y-2">
          {weeklyLoading && <p className="text-muted-foreground text-sm">Yükleniyor…</p>}
          {weeklyMenu && weeklyMenu.length === 0 && (
            <p className="text-muted-foreground text-sm">Bu hafta için menü yok.</p>
          )}
          {weeklyMenu?.map((menu) => (
            <div key={menu.id} className="flex items-center justify-between rounded-lg border p-3">
              <span className="text-sm font-medium">{formatDate(menu.date)}</span>
              <Button
                size="sm"
                variant="outline"
                onClick={() => {
                  if (confirm(`${formatDate(menu.date)} menüsü silinsin mi?`)) {
                    deleteMutation.mutate(menu.id);
                  }
                }}
                disabled={deleteMutation.isPending}
              >
                <Trash2 />
                Sil
              </Button>
            </div>
          ))}
        </CardContent>
      </Card>
    </div>
  );
}
