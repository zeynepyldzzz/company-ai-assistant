import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
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
import { createSurvey } from "@/api/survey";

// C-8 (#52): POST /admin/surveys — taslak (published=false) olarak olusturur.
// Yayimlama ayri bir adim (liste sayfasindaki "Yayımla" butonu).
export function SurveyCreateSheet() {
  const { token } = useAuth();
  const queryClient = useQueryClient();

  const [open, setOpen] = useState(false);
  const [title, setTitle] = useState("");

  const mutation = useMutation({
    mutationFn: () => createSurvey({ title: title.trim() }, token!),
    onSuccess: () => {
      toast.success("Anket oluşturuldu (taslak). Yayımlamayı unutmayın.");
      queryClient.invalidateQueries({ queryKey: ["admin", "surveys"] });
      setTitle("");
      setOpen(false);
    },
    onError: (error) => {
      const message = error instanceof ApiError ? error.message : "Anket oluşturulamadı.";
      toast.error(message);
    },
  });

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    if (!title.trim()) {
      toast.error("Anket başlığı boş olamaz.");
      return;
    }
    mutation.mutate();
  }

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      <SheetTrigger
        render={
          <Button>
            <Plus />
            Yeni Anket
          </Button>
        }
      />
      <SheetContent className="w-full overflow-y-auto sm:max-w-lg">
        <SheetHeader>
          <SheetTitle>Yeni Anket</SheetTitle>
          <SheetDescription>
            Anket taslak olarak oluşturulur; çalışanlar görene kadar listede "Yayımla" ile
            yayımlamanız gerekir.
          </SheetDescription>
        </SheetHeader>

        <form onSubmit={handleSubmit} className="flex flex-1 flex-col gap-4 px-4">
          <div className="space-y-1.5">
            <Label htmlFor="survey-title">Başlık</Label>
            <Input
              id="survey-title"
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              placeholder="Örn. Memnuniyet Anketi 2026 Q3"
            />
          </div>

          <SheetFooter className="px-0">
            <Button type="submit" disabled={mutation.isPending}>
              {mutation.isPending ? "Kaydediliyor…" : "Oluştur"}
            </Button>
          </SheetFooter>
        </form>
      </SheetContent>
    </Sheet>
  );
}
