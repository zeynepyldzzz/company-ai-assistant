import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { BarChart3 } from "lucide-react";
import type { AdminSurvey } from "@company/shared";
import { Button } from "@/components/ui/button";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";
import { useAuth } from "@/auth/auth-context";
import { getSurveyResults } from "@/api/survey";

// C-8 (#52) FR-44: yetkili kullanicilar anket sonuclarini gorebilmeli.
// "Ozet/grafik halinde": her soru icin cevap -> kac kisi verdigi basit bir
// bar-chart olarak cizilir (ekstra bir grafik kutuphanesi eklemeye gerek yok).
export function SurveyResultsSheet({ survey }: { survey: AdminSurvey }) {
  const { token } = useAuth();
  const [open, setOpen] = useState(false);

  const { data, isLoading, isError } = useQuery({
    queryKey: ["admin", "surveys", survey.id, "results"],
    queryFn: () => getSurveyResults(survey.id, token!),
    enabled: Boolean(token) && open,
  });

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      <SheetTrigger
        render={
          <Button variant="outline" size="sm">
            <BarChart3 />
            Sonuçlar
          </Button>
        }
      />
      <SheetContent className="w-full overflow-y-auto sm:max-w-lg">
        <SheetHeader>
          <SheetTitle>{survey.title}</SheetTitle>
          <SheetDescription>Anket sonuçları (özet).</SheetDescription>
        </SheetHeader>

        <div className="flex flex-1 flex-col gap-6 px-4">
          {isLoading && <p className="text-muted-foreground text-sm">Yükleniyor…</p>}
          {isError && <p className="text-destructive text-sm">Sonuçlar yüklenemedi.</p>}

          {data && (
            <>
              <div className="flex gap-6 text-sm">
                <div>
                  <div className="text-muted-foreground">Toplam Yanıt</div>
                  <div className="text-lg font-semibold">{data.totalResponses}</div>
                </div>
                <div>
                  <div className="text-muted-foreground">Anonim Geri Bildirim</div>
                  <div className="text-lg font-semibold">{data.totalFeedback}</div>
                </div>
              </div>

              {Object.keys(data.answerCounts).length === 0 ? (
                <p className="text-muted-foreground text-sm">Henüz yanıt gönderilmemiş.</p>
              ) : (
                <div className="space-y-1.5">
                  {Object.entries(data.answerCounts).map(([option, count]) => {
                    const max = Math.max(...Object.values(data.answerCounts), 1);
                    return (
                      <div key={option} className="flex items-center gap-2 text-xs">
                        <span className="w-24 shrink-0 truncate" title={option}>
                          {option}
                        </span>
                        <div className="bg-muted h-4 flex-1 overflow-hidden rounded">
                          <div
                            className="bg-primary h-full rounded"
                            style={{ width: `${(count / max) * 100}%` }}
                          />
                        </div>
                        <span className="text-muted-foreground w-6 shrink-0 text-right">
                          {count}
                        </span>
                      </div>
                    );
                  })}
                </div>
              )}

              <div className="space-y-2">
                <div className="text-sm font-medium">Anonim Geri Bildirimler</div>
                {data.feedbackComments.length === 0 ? (
                  <p className="text-muted-foreground text-sm">Henüz geri bildirim yok.</p>
                ) : (
                  <ul className="space-y-1.5 text-sm">
                    {data.feedbackComments.map((comment, index) => (
                      <li key={index} className="bg-muted/50 rounded-md px-3 py-2">
                        {comment}
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            </>
          )}
        </div>
      </SheetContent>
    </Sheet>
  );
}
