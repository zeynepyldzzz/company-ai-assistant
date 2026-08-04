import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/auth/auth-context";
import { ApiError } from "@/api/client";
import { listAdminSurveys, publishSurvey, deleteSurvey } from "@/api/survey";
import { SurveyCreateSheet } from "./survey-create-sheet";
import { SurveyEditSheet } from "./survey-edit-sheet";
import { SurveyResultsSheet } from "./survey-results-sheet";

function formatDate(value: string): string {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString("tr-TR");
}

// C-8 (#52): Admin anket oluşturma + yayımlama + sonuç görüntüleme (FR-44, FR-76).
// C-13 (#121): düzenleme (mavi), silme (kırmızı) ve geçerlilik (deadline) tarihi eklendi.
export function AdminSurveysPage() {
  const { token } = useAuth();
  const queryClient = useQueryClient();

  const { data, isLoading, isError } = useQuery({
    queryKey: ["admin", "surveys"],
    queryFn: () => listAdminSurveys(token!),
    enabled: Boolean(token),
  });

  const publishMutation = useMutation({
    mutationFn: (id: number) => publishSurvey(id, token!),
    onSuccess: () => {
      toast.success("Anket yayımlandı.");
      queryClient.invalidateQueries({ queryKey: ["admin", "surveys"] });
    },
    onError: (error) => {
      const message = error instanceof ApiError ? error.message : "Anket yayımlanamadı.";
      toast.error(message);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deleteSurvey(id, token!),
    onSuccess: () => {
      toast.success("Anket silindi.");
      queryClient.invalidateQueries({ queryKey: ["admin", "surveys"] });
    },
    onError: (error) => {
      const message = error instanceof ApiError ? error.message : "Anket silinemedi.";
      toast.error(message);
    },
  });

  function handleDelete(id: number, title: string) {
    if (window.confirm(`"${title}" anketini silmek istediğinize emin misiniz? Bu işlem geri alınamaz.`)) {
      deleteMutation.mutate(id);
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-xl font-semibold">Anketler</h1>
          <p className="text-muted-foreground text-sm">
            Anket oluşturun, yayımlayın ve sonuçları görüntüleyin.
          </p>
        </div>
        <SurveyCreateSheet />
      </div>

      {isLoading && <p className="text-muted-foreground text-sm">Yükleniyor…</p>}
      {isError && <p className="text-destructive text-sm">Anketler yüklenemedi.</p>}

      {data && (
        <div className="overflow-x-auto rounded-lg border">
          <table className="w-full text-sm">
            <thead className="bg-muted/50">
              <tr>
                <th className="px-4 py-2 text-left font-medium">Başlık</th>
                <th className="px-4 py-2 text-center font-medium">Durum</th>
                <th className="px-4 py-2 text-left font-medium">Oluşturulma</th>
                <th className="px-4 py-2 text-left font-medium">Geçerlilik (Son Yanıt)</th>
                <th className="px-4 py-2 text-right font-medium">İşlemler</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {data.length === 0 ? (
                <tr>
                  <td colSpan={5} className="text-muted-foreground px-4 py-3 text-center">
                    Henüz anket yok.
                  </td>
                </tr>
              ) : (
                data.map((survey) => (
                  <tr key={survey.id}>
                    <td className="px-4 py-2 font-medium">{survey.title}</td>
                    <td className="px-4 py-2 text-center">
                      <span
                        className={
                          survey.published
                            ? "bg-success-soft text-success rounded-full px-2 py-0.5 text-xs font-semibold"
                            : "bg-muted text-muted-foreground rounded-full px-2 py-0.5 text-xs font-semibold"
                        }
                      >
                        {survey.published ? "Yayımlandı" : "Taslak"}
                      </span>
                    </td>
                    <td className="text-muted-foreground px-4 py-2">
                      {formatDate(survey.createdAt)}
                    </td>
                    <td className="text-muted-foreground px-4 py-2">
                      {survey.deadline ? formatDate(survey.deadline) : "Süresiz"}
                    </td>
                    <td className="px-4 py-2">
                      <div className="flex justify-end gap-2">
                        {!survey.published && (
                          <Button
                            size="sm"
                            onClick={() => publishMutation.mutate(survey.id)}
                            disabled={publishMutation.isPending}
                          >
                            Yayımla
                          </Button>
                        )}
                        <SurveyEditSheet survey={survey} />
                        <Button
                          size="sm"
                          variant="outline"
                          className="border-red-300 text-red-600 hover:bg-red-50 hover:text-red-700"
                          onClick={() => handleDelete(survey.id, survey.title)}
                          disabled={deleteMutation.isPending}
                        >
                          <Trash2 className="size-4" />
                          Sil
                        </Button>
                        <SurveyResultsSheet survey={survey} />
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
