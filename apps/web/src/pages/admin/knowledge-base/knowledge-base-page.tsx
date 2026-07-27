import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Trash2 } from "lucide-react";
import type { DocumentSummary } from "@company/shared";
import { Button } from "@/components/ui/button";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { PaginationControls } from "@/components/pagination-controls";
import { useAuth } from "@/auth/auth-context";
import { ApiError } from "@/api/client";
import { listDocuments, deleteDocument } from "@/api/knowledge-base";
import { DocumentCreateSheet } from "./document-create-sheet";
import { VersionHistorySheet } from "./version-history-sheet";

const PAGE_SIZE = 12;

function formatDate(value: string | null): string {
  if (!value) return "—";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleDateString("tr-TR");
}

export function KnowledgeBasePage() {
  const { token } = useAuth();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);

  const { data, isLoading, isError } = useQuery({
    queryKey: ["knowledge-base", "documents", page],
    queryFn: () => listDocuments({ page, pageSize: PAGE_SIZE }, token!),
    enabled: Boolean(token),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deleteDocument(id, token!),
    onSuccess: () => {
      toast.success("Doküman silindi.");
      queryClient.invalidateQueries({ queryKey: ["knowledge-base", "documents"] });
    },
    onError: (error) => {
      const message = error instanceof ApiError ? error.message : "Doküman silinemedi.";
      toast.error(message);
    },
  });

  function handleDelete(doc: DocumentSummary) {
    if (window.confirm(`"${doc.title}" dokümanı ve tüm versiyonları silinecek. Onaylıyor musunuz?`)) {
      deleteMutation.mutate(doc.id);
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-xl font-semibold">Bilgi Tabanı</h1>
          <p className="text-muted-foreground text-sm">
            İK doküman ve politikalarının versiyon yönetimi.
          </p>
        </div>
        <DocumentCreateSheet />
      </div>

      {isLoading && <p className="text-muted-foreground text-sm">Yükleniyor…</p>}
      {isError && <p className="text-destructive text-sm">Dokümanlar yüklenemedi.</p>}

      {data && (
        <>
          <div className="rounded-lg border">
            <Table>
              <TableHeader className="bg-muted/50">
                <TableRow>
                  <TableHead>Başlık</TableHead>
                  <TableHead>Kategori</TableHead>
                  <TableHead className="text-center">Güncel Versiyon</TableHead>
                  <TableHead>Yürürlük Tarihi</TableHead>
                  <TableHead>Oluşturulma</TableHead>
                  <TableHead className="text-right">İşlemler</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.data.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={6} className="text-muted-foreground text-center">
                      Henüz doküman yok.
                    </TableCell>
                  </TableRow>
                ) : (
                  data.data.map((doc) => (
                    <TableRow key={doc.id}>
                      <TableCell className="font-medium">{doc.title}</TableCell>
                      <TableCell className="text-muted-foreground">{doc.procedureCategory}</TableCell>
                      <TableCell className="text-center">
                        {doc.currentVersionNo ? `v${doc.currentVersionNo}` : "—"}
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        {formatDate(doc.currentEffectiveDate)}
                      </TableCell>
                      <TableCell className="text-muted-foreground">{formatDate(doc.createdAt)}</TableCell>
                      <TableCell>
                        <div className="flex justify-end gap-2">
                          <VersionHistorySheet doc={doc} />
                          <Button
                            variant="destructive"
                            size="sm"
                            onClick={() => handleDelete(doc)}
                            disabled={deleteMutation.isPending}
                          >
                            <Trash2 />
                            Sil
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </div>
          <PaginationControls
            page={data.page}
            pageSize={data.pageSize}
            total={data.total}
            onPageChange={setPage}
          />
        </>
      )}
    </div>
  );
}
