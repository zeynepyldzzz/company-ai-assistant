import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Trash2 } from "lucide-react";
import type { DocumentSummary } from "@company/shared";
import { Button } from "@/components/ui/button";
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
          <div className="overflow-x-auto rounded-lg border">
            <table className="w-full text-sm">
              <thead className="bg-muted/50">
                <tr>
                  <th className="px-4 py-2 text-left font-medium">Başlık</th>
                  <th className="px-4 py-2 text-left font-medium">Kategori</th>
                  <th className="px-4 py-2 text-center font-medium">Güncel Versiyon</th>
                  <th className="px-4 py-2 text-left font-medium">Yürürlük Tarihi</th>
                  <th className="px-4 py-2 text-left font-medium">Oluşturulma</th>
                  <th className="px-4 py-2 text-right font-medium">İşlemler</th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {data.data.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="text-muted-foreground px-4 py-3 text-center">
                      Henüz doküman yok.
                    </td>
                  </tr>
                ) : (
                  data.data.map((doc) => (
                    <tr key={doc.id}>
                      <td className="px-4 py-2 font-medium">{doc.title}</td>
                      <td className="text-muted-foreground px-4 py-2">{doc.procedureCategory}</td>
                      <td className="px-4 py-2 text-center">
                        {doc.currentVersionNo ? `v${doc.currentVersionNo}` : "—"}
                      </td>
                      <td className="text-muted-foreground px-4 py-2">
                        {formatDate(doc.currentEffectiveDate)}
                      </td>
                      <td className="text-muted-foreground px-4 py-2">{formatDate(doc.createdAt)}</td>
                      <td className="px-4 py-2">
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
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
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
