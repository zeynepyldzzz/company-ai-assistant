import { Fragment, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { History } from "lucide-react";
import type { DocumentSummary, VersionCreateRequest } from "@company/shared";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Separator } from "@/components/ui/separator";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";
import { useAuth } from "@/auth/auth-context";
import { ApiError } from "@/api/client";
import { addVersion, listVersions } from "@/api/knowledge-base";
import { StepsEditor, toProcedureSteps, type StepDraft } from "./steps-editor";

function formatDate(value: string): string {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleDateString("tr-TR");
}

export function VersionHistorySheet({ doc }: { doc: DocumentSummary }) {
  const { token } = useAuth();
  const queryClient = useQueryClient();

  const [open, setOpen] = useState(false);
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [effectiveDate, setEffectiveDate] = useState("");
  const [content, setContent] = useState("");
  const [steps, setSteps] = useState<StepDraft[]>([]);

  // Versiyon gecmisi yalnizca sheet acikken cekilir.
  const { data, isLoading, isError } = useQuery({
    queryKey: ["knowledge-base", "versions", doc.id],
    queryFn: () => listVersions(doc.id, token!),
    enabled: Boolean(token) && open,
  });

  const mutation = useMutation({
    mutationFn: (body: VersionCreateRequest) => addVersion(doc.id, body, token!),
    onSuccess: () => {
      toast.success("Yeni versiyon eklendi.");
      // Hem bu dokumanin versiyonlari hem de liste (guncel versiyon no) tazelenir.
      queryClient.invalidateQueries({ queryKey: ["knowledge-base", "versions", doc.id] });
      queryClient.invalidateQueries({ queryKey: ["knowledge-base", "documents"] });
      setEffectiveDate("");
      setContent("");
      setSteps([]);
    },
    onError: (error) => {
      const message = error instanceof ApiError ? error.message : "Versiyon eklenemedi.";
      toast.error(message);
    },
  });

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    if (!effectiveDate) {
      toast.error("Yürürlük tarihi zorunlu.");
      return;
    }
    const cleanedSteps = toProcedureSteps(steps);
    mutation.mutate({
      effectiveDate,
      content: content.trim() || undefined,
      steps: cleanedSteps.length > 0 ? cleanedSteps : undefined,
    });
  }

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      <SheetTrigger
        render={
          <Button variant="outline" size="sm">
            <History />
            Versiyonlar
          </Button>
        }
      />
      <SheetContent className="w-full overflow-y-auto sm:max-w-lg">
        <SheetHeader>
          <SheetTitle>{doc.title}</SheetTitle>
          <SheetDescription>
            Versiyon geçmişi ve yeni versiyon ekleme. İçeriği görmek için bir versiyona tıklayın.
          </SheetDescription>
        </SheetHeader>

        <div className="flex flex-1 flex-col gap-4 px-4">
          {isLoading && <p className="text-muted-foreground text-sm">Yükleniyor…</p>}
          {isError && <p className="text-destructive text-sm">Versiyonlar yüklenemedi.</p>}

          {data && (
            <div className="rounded-lg border">
              <Table>
                <TableHeader className="bg-muted/50">
                  <TableRow>
                    <TableHead className="text-center">Ver.</TableHead>
                    <TableHead>Yürürlük</TableHead>
                    <TableHead className="text-center">Adım</TableHead>
                    <TableHead>Durum</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data.data.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={4} className="text-muted-foreground text-center">
                        Versiyon yok.
                      </TableCell>
                    </TableRow>
                  ) : (
                    data.data.map((version) => (
                      <Fragment key={version.id}>
                        <TableRow
                          className="cursor-pointer"
                          onClick={() =>
                            setExpandedId((prev) => (prev === version.id ? null : version.id))
                          }
                        >
                          <TableCell className="text-center font-medium">v{version.versionNo}</TableCell>
                          <TableCell className="text-muted-foreground">
                            {formatDate(version.effectiveDate)}
                          </TableCell>
                          <TableCell className="text-muted-foreground text-center">
                            {version.steps?.length ?? 0}
                          </TableCell>
                          <TableCell>
                            {version.isCurrent ? (
                              <span className="text-primary font-medium">Güncel</span>
                            ) : (
                              <span className="text-muted-foreground">Eski</span>
                            )}
                          </TableCell>
                        </TableRow>
                        {expandedId === version.id && (
                          <TableRow className="bg-muted/20 hover:bg-muted/20">
                            <TableCell colSpan={4} className="space-y-3 whitespace-normal">
                              <div>
                                <p className="text-muted-foreground mb-1 text-xs font-medium uppercase">
                                  İçerik
                                </p>
                                {version.content ? (
                                  <p className="text-sm whitespace-pre-wrap">{version.content}</p>
                                ) : (
                                  <p className="text-muted-foreground text-sm">—</p>
                                )}
                              </div>
                              <div>
                                <p className="text-muted-foreground mb-1 text-xs font-medium uppercase">
                                  Adımlar
                                </p>
                                {version.steps && version.steps.length > 0 ? (
                                  <ol className="space-y-1.5">
                                    {version.steps.map((step) => (
                                      <li key={step.order} className="text-sm">
                                        <span className="font-medium">
                                          {step.order}. {step.title}
                                        </span>
                                        {step.detail && (
                                          <span className="text-muted-foreground"> — {step.detail}</span>
                                        )}
                                      </li>
                                    ))}
                                  </ol>
                                ) : (
                                  <p className="text-muted-foreground text-sm">Adım yok.</p>
                                )}
                              </div>
                            </TableCell>
                          </TableRow>
                        )}
                      </Fragment>
                    ))
                  )}
                </TableBody>
              </Table>
            </div>
          )}

          <Separator />

          <form onSubmit={handleSubmit} className="space-y-4">
            <h3 className="text-sm font-semibold">Yeni Versiyon</h3>
            <div className="space-y-1.5">
              <Label htmlFor="version-effective-date">Yürürlük Tarihi</Label>
              <Input
                id="version-effective-date"
                type="date"
                value={effectiveDate}
                onChange={(event) => setEffectiveDate(event.target.value)}
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="version-content">İçerik</Label>
              <textarea
                id="version-content"
                value={content}
                onChange={(event) => setContent(event.target.value)}
                rows={4}
                placeholder="Serbest metin içerik (opsiyonel)"
                className="border-input placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-ring/50 w-full rounded-lg border bg-transparent px-2.5 py-1.5 text-sm outline-none focus-visible:ring-3 dark:bg-input/30"
              />
            </div>
            <StepsEditor steps={steps} onChange={setSteps} />
            <Button type="submit" disabled={mutation.isPending}>
              {mutation.isPending ? "Kaydediliyor…" : "Versiyon Ekle"}
            </Button>
          </form>
        </div>
      </SheetContent>
    </Sheet>
  );
}
