import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Plus, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { FIELD_LIMITS } from "@/lib/field-limits";
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
// C-13 (#121): deadline (opsiyonel) ve sabit secenek listesi (min 2) eklendi.
export function SurveyCreateSheet() {
  const { token } = useAuth();
  const queryClient = useQueryClient();

  const [open, setOpen] = useState(false);
  const [title, setTitle] = useState("");
  const [deadline, setDeadline] = useState("");
  const [options, setOptions] = useState(["", ""]);

  const mutation = useMutation({
    mutationFn: () =>
      createSurvey(
        {
          title: title.trim(),
          deadline: deadline ? new Date(deadline).toISOString() : null,
          options: options.map((o) => o.trim()).filter(Boolean),
        },
        token!
      ),
    onSuccess: () => {
      toast.success("Anket oluşturuldu (taslak). Yayımlamayı unutmayın.");
      queryClient.invalidateQueries({ queryKey: ["admin", "surveys"] });
      setTitle("");
      setDeadline("");
      setOptions(["", ""]);
      setOpen(false);
    },
    onError: (error) => {
      const message = error instanceof ApiError ? error.message : "Anket oluşturulamadı.";
      toast.error(message);
    },
  });

  function updateOption(index: number, value: string) {
    setOptions((prev) => prev.map((o, i) => (i === index ? value : o)));
  }

  function removeOption(index: number) {
    setOptions((prev) => prev.filter((_, i) => i !== index));
  }

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    if (!title.trim()) {
      toast.error("Anket başlığı boş olamaz.");
      return;
    }
    const filled = options.map((o) => o.trim()).filter(Boolean);
    if (filled.length < 2) {
      toast.error("En az 2 seçenek girilmeli.");
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
            <Label htmlFor="survey-title">Başlık (soru)</Label>
            <Input
              id="survey-title"
              maxLength={FIELD_LIMITS.surveyTitle}
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              placeholder="Örn. Yeni kafeterya menüsünden memnun musunuz?"
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="survey-deadline">Son yanıt tarihi (opsiyonel)</Label>
            <Input
              id="survey-deadline"
              type="datetime-local"
              value={deadline}
              onChange={(event) => setDeadline(event.target.value)}
            />
          </div>

          <div className="space-y-2">
            <Label>Seçenekler (en az 2)</Label>
            {options.map((option, index) => (
              <div key={index} className="flex items-center gap-2">
                <Input
                  value={option}
                  maxLength={FIELD_LIMITS.surveyOption}
                  onChange={(event) => updateOption(index, event.target.value)}
                  placeholder={`Seçenek ${index + 1}`}
                />
                {options.length > 2 && (
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    onClick={() => removeOption(index)}
                  >
                    <X className="size-4" />
                  </Button>
                )}
              </div>
            ))}
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => setOptions((prev) => [...prev, ""])}
            >
              <Plus className="size-4" />
              Seçenek Ekle
            </Button>
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
