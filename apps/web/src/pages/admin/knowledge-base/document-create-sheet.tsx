import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Plus } from "lucide-react";
import type { DocumentCreateRequest } from "@company/shared";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
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
import { createDocument, listProcedures } from "@/api/knowledge-base";
import { StepsEditor, toProcedureSteps, type StepDraft } from "./steps-editor";

export function DocumentCreateSheet() {
  const { token } = useAuth();
  const queryClient = useQueryClient();

  const [open, setOpen] = useState(false);
  const [procedureId, setProcedureId] = useState<number | null>(null);
  const [title, setTitle] = useState("");
  const [effectiveDate, setEffectiveDate] = useState("");
  const [content, setContent] = useState("");
  const [steps, setSteps] = useState<StepDraft[]>([]);

  // Prosedur listesi yalnizca sheet acikken cekilir (procedureId dropdown'i).
  const { data: procedures } = useQuery({
    queryKey: ["hr", "procedures"],
    queryFn: () => listProcedures(token!),
    enabled: Boolean(token) && open,
  });

  const mutation = useMutation({
    mutationFn: (body: DocumentCreateRequest) => createDocument(body, token!),
    onSuccess: () => {
      toast.success("Doküman oluşturuldu.");
      queryClient.invalidateQueries({ queryKey: ["knowledge-base", "documents"] });
      resetAndClose();
    },
    onError: (error) => {
      const message = error instanceof ApiError ? error.message : "Doküman oluşturulamadı.";
      toast.error(message);
    },
  });

  function resetAndClose() {
    setProcedureId(null);
    setTitle("");
    setEffectiveDate("");
    setContent("");
    setSteps([]);
    setOpen(false);
  }

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    if (procedureId === null) {
      toast.error("Prosedür seçilmeli.");
      return;
    }
    if (!title.trim() || !effectiveDate) {
      toast.error("Başlık ve yürürlük tarihi zorunlu.");
      return;
    }
    const cleanedSteps = toProcedureSteps(steps);
    mutation.mutate({
      procedureId,
      title: title.trim(),
      effectiveDate,
      content: content.trim() || undefined,
      steps: cleanedSteps.length > 0 ? cleanedSteps : undefined,
    });
  }

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      <SheetTrigger
        render={
          <Button>
            <Plus />
            Yeni Doküman
          </Button>
        }
      />
      <SheetContent className="w-full overflow-y-auto sm:max-w-lg">
        <SheetHeader>
          <SheetTitle>Yeni Doküman</SheetTitle>
          <SheetDescription>
            Bir prosedüre bağlı yeni doküman ve ilk versiyonu (v1) oluşturur.
          </SheetDescription>
        </SheetHeader>

        <form onSubmit={handleSubmit} className="flex flex-1 flex-col gap-4 px-4">
          <div className="space-y-1.5">
            <Label>Prosedür</Label>
            <Select<number | null>
              value={procedureId}
              onValueChange={(value) => setProcedureId(value)}
            >
              <SelectTrigger>
                <SelectValue placeholder="Prosedür seçin…" />
              </SelectTrigger>
              <SelectContent>
                {procedures?.map((p) => (
                  <SelectItem key={p.id} value={p.id}>
                    {p.title} ({p.category})
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="doc-title">Başlık</Label>
            <Input
              id="doc-title"
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              placeholder="Doküman başlığı"
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="doc-effective-date">Yürürlük Tarihi</Label>
            <Input
              id="doc-effective-date"
              type="date"
              value={effectiveDate}
              onChange={(event) => setEffectiveDate(event.target.value)}
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="doc-content">İçerik</Label>
            <textarea
              id="doc-content"
              value={content}
              onChange={(event) => setContent(event.target.value)}
              rows={4}
              placeholder="Serbest metin içerik (opsiyonel)"
              className="border-input placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-ring/50 w-full rounded-lg border bg-transparent px-2.5 py-1.5 text-sm outline-none focus-visible:ring-3 dark:bg-input/30"
            />
          </div>

          <StepsEditor steps={steps} onChange={setSteps} />

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
