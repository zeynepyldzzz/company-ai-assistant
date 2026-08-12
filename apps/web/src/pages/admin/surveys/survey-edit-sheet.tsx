import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Pencil, Plus, X } from "lucide-react";
import type { AdminSurvey } from "@company/shared";
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
import { updateSurvey } from "@/api/survey";

// C-13 (#121): admin mevcut anketin basligini, seceneklerini ve gecerlilik
// (deadline) tarihini duzenleyebilir. "Duzenle" butonu acik mavi.
function toDatetimeLocal(value: string | null): string {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

export function SurveyEditSheet({ survey }: { survey: AdminSurvey }) {
  const { token } = useAuth();
  const queryClient = useQueryClient();

  const [open, setOpen] = useState(false);
  const [title, setTitle] = useState(survey.title);
  const [deadline, setDeadline] = useState(toDatetimeLocal(survey.deadline));
  const [options, setOptions] = useState(
    survey.options.length > 0 ? survey.options.map((o) => o.optionText) : ["", ""]
  );

  function resetFromSurvey() {
    setTitle(survey.title);
    setDeadline(toDatetimeLocal(survey.deadline));
    setOptions(survey.options.length > 0 ? survey.options.map((o) => o.optionText) : ["", ""]);
  }

  const mutation = useMutation({
    mutationFn: () =>
      updateSurvey(
        survey.id,
        {
          title: title.trim(),
          deadline: deadline ? new Date(deadline).toISOString() : null,
          options: options.map((o) => o.trim()).filter(Boolean),
        },
        token!
      ),
    onSuccess: () => {
      toast.success("Anket güncellendi.");
      queryClient.invalidateQueries({ queryKey: ["admin", "surveys"] });
      setOpen(false);
    },
    onError: (error) => {
      const message = error instanceof ApiError ? error.message : "Anket güncellenemedi.";
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
    <Sheet
      open={open}
      onOpenChange={(next) => {
        setOpen(next);
        if (next) resetFromSurvey();
      }}
    >
      <SheetTrigger
        render={
          <Button
            size="sm"
            variant="outline"
            className="border-sky-300 text-sky-600 hover:bg-sky-50 hover:text-sky-700"
          >
            <Pencil className="size-4" />
            Düzenle
          </Button>
        }
      />
      <SheetContent className="w-full overflow-y-auto sm:max-w-lg">
        <SheetHeader>
          <SheetTitle>Anketi Düzenle</SheetTitle>
          <SheetDescription>
            Başlığı, seçenekleri ve geçerlilik (son yanıt) tarihini güncelleyebilirsiniz.
          </SheetDescription>
        </SheetHeader>

        <form onSubmit={handleSubmit} className="flex flex-1 flex-col gap-4 px-4">
          <div className="space-y-1.5">
            <Label htmlFor="survey-edit-title">Başlık (soru)</Label>
            <Input
              id="survey-edit-title"
              maxLength={FIELD_LIMITS.surveyTitle}
              value={title}
              onChange={(event) => setTitle(event.target.value)}
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="survey-edit-deadline">Geçerlilik (son yanıt) tarihi</Label>
            <Input
              id="survey-edit-deadline"
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
              {mutation.isPending ? "Kaydediliyor…" : "Kaydet"}
            </Button>
          </SheetFooter>
        </form>
      </SheetContent>
    </Sheet>
  );
}
