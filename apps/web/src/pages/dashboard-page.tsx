import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Link } from "react-router";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import {
  Sheet,
  SheetClose,
  SheetContent,
  SheetDescription,
  SheetFooter,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";
import { useAuth } from "@/auth/auth-context";
import { useMe } from "@/auth/use-me";
import { getTodayMenu } from "@/api/menu";
import { getMySchedule, getMySummary } from "@/api/schedule";
import { listActiveSurveys, submitSurveyResponse, type ActiveSurvey } from "@/api/survey";
import { getActiveAnnouncements } from "@/api/announcement";
import type { ScheduleDay } from "@company/shared";
import { ApiError } from "@/api/client";
import { cn } from "@/lib/utils";

const dayLabels: Record<ScheduleDay["day"], string> = {
  monday: "Pzt",
  tuesday: "Sal",
  wednesday: "Çar",
  thursday: "Per",
  friday: "Cum",
};

const statusStyles: Record<ScheduleDay["status"], string> = {
  office: "bg-success-soft text-success",
  remote: "bg-warning-soft text-warning",
  leave: "bg-danger-soft text-danger",
};

const statusLabels: Record<ScheduleDay["status"], string> = {
  office: "Ofiste",
  remote: "Uzaktan",
  leave: "İzinli",
};

function SurveyRespondSheet({ survey }: { survey: ActiveSurvey }) {
  const { token } = useAuth();
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const [answer, setAnswer] = useState("");

  const mutation = useMutation({
    mutationFn: () => submitSurveyResponse(survey.id, answer.trim(), token!),
    onSuccess: () => {
      toast.success("Yanıtınız kaydedildi.");
      setOpen(false);
      setAnswer("");
      queryClient.invalidateQueries({ queryKey: ["surveys", "active"] });
    },
    onError: (err) => {
      toast.error(err instanceof ApiError ? err.message : "Yanıt gönderilemedi.");
    },
  });

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      <SheetTrigger render={<Button size="sm" variant="outline">Yanıtla</Button>} />
      <SheetContent>
        <SheetHeader>
          <SheetTitle>{survey.title}</SheetTitle>
          <SheetDescription>Görüşünüzü kısaca yazın.</SheetDescription>
        </SheetHeader>
        <div className="px-4">
          <Textarea
            value={answer}
            onChange={(e) => setAnswer(e.target.value)}
            placeholder="Yanıtınız…"
            rows={5}
            autoFocus
          />
        </div>
        <SheetFooter>
          <Button
            onClick={() => mutation.mutate()}
            disabled={!answer.trim() || mutation.isPending}
          >
            {mutation.isPending ? "Gönderiliyor…" : "Gönder"}
          </Button>
          <SheetClose render={<Button variant="outline">İptal</Button>} />
        </SheetFooter>
      </SheetContent>
    </Sheet>
  );
}

export function DashboardPage() {
  const { token } = useAuth();
  const { data: me } = useMe();

  const announcementsQuery = useQuery({
    queryKey: ["announcements", "active"],
    queryFn: () => getActiveAnnouncements(token!),
    enabled: Boolean(token),
  });

  const surveysQuery = useQuery({
    queryKey: ["surveys", "active"],
    queryFn: () => listActiveSurveys(token!),
    enabled: Boolean(token),
  });

  const menuQuery = useQuery({
    queryKey: ["menu", "today"],
    queryFn: () => getTodayMenu(token!),
    enabled: Boolean(token),
  });

  const scheduleQuery = useQuery({
    queryKey: ["schedule", "me"],
    queryFn: () => getMySchedule(token!),
    enabled: Boolean(token),
  });

  const summaryQuery = useQuery({
    queryKey: ["schedule", "me", "summary"],
    queryFn: () => getMySummary(token!),
    enabled: Boolean(token),
  });

  const firstName = me?.name.split(" ")[0];

  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-2xl font-extrabold">
          Hoş geldin{firstName ? `, ${firstName}` : ""} 👋
        </h2>
      </div>

      <section className="space-y-3">
        <p className="text-muted-foreground text-[11px] font-bold tracking-[0.04em] uppercase">
          Duyurular
        </p>
        {announcementsQuery.isLoading && (
          <p className="text-muted-foreground text-sm">Yükleniyor…</p>
        )}
        {announcementsQuery.data?.length === 0 && (
          <p className="text-muted-foreground text-sm">Şu anda aktif duyuru yok.</p>
        )}
        {announcementsQuery.data && announcementsQuery.data.length > 0 && (
          <div className="space-y-3">
            {announcementsQuery.data.map((announcement) => (
              <Card key={announcement.id}>
                <CardContent className="space-y-1 py-1">
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-medium">{announcement.title}</span>
                    {announcement.pinned && (
                      <span className="rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-800">
                        Sabitlendi
                      </span>
                    )}
                  </div>
                  <p className="text-muted-foreground text-sm">{announcement.content}</p>
                </CardContent>
              </Card>
            ))}
          </div>
        )}
      </section>

      <section className="space-y-3">
        <p className="text-muted-foreground text-[11px] font-bold tracking-[0.04em] uppercase">
          Açık Anketler
        </p>
        {surveysQuery.isLoading && (
          <p className="text-muted-foreground text-sm">Yükleniyor…</p>
        )}
        {surveysQuery.data?.length === 0 && (
          <p className="text-muted-foreground text-sm">Şu anda açık anket yok.</p>
        )}
        {surveysQuery.data && surveysQuery.data.length > 0 && (
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {surveysQuery.data.map((survey) => (
              <Card key={survey.id}>
                <CardContent className="flex items-center justify-between gap-3 py-1">
                  <span className="text-sm font-medium">{survey.title}</span>
                  <SurveyRespondSheet survey={survey} />
                </CardContent>
              </Card>
            ))}
          </div>
        )}
      </section>

      <div className="grid gap-4 lg:grid-cols-2">
        <Card className="gap-0 p-0">
          <CardHeader className="border-b px-4 py-3">
            <CardTitle>Bugünkü Menü</CardTitle>
          </CardHeader>
          <CardContent className="p-0">
            {menuQuery.isLoading && (
              <p className="text-muted-foreground px-4 py-3 text-sm">Yükleniyor…</p>
            )}
            {menuQuery.data && menuQuery.data.items.length === 0 && (
              <p className="text-muted-foreground px-4 py-3 text-sm">Bugün için menü girilmemiş.</p>
            )}
            {menuQuery.data && menuQuery.data.items.length > 0 && (
              <ul className="divide-y">
                {menuQuery.data.items.map((item) => (
                  <li key={item.id} className="px-4 py-2.5 text-[15px] font-medium">
                    {item.name}
                  </li>
                ))}
              </ul>
            )}
          </CardContent>
        </Card>

        <Card className="gap-0 p-0">
          <CardHeader className="border-b px-4 py-3">
            <CardTitle>Haftalık Düzenim</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3 p-4">
            {scheduleQuery.isLoading && (
              <p className="text-muted-foreground text-sm">Yükleniyor…</p>
            )}
            {scheduleQuery.data && scheduleQuery.data.days.length === 0 && (
              <p className="text-muted-foreground text-sm">
                Bu hafta için düzen girilmemiş.{" "}
                <Link to="/my-schedule" className="text-primary underline underline-offset-2">
                  Düzenle
                </Link>
              </p>
            )}
            {scheduleQuery.data && scheduleQuery.data.days.length > 0 && (
              <div className="flex gap-2">
                {scheduleQuery.data.days.map((day) => (
                  <div key={day.day} className="flex flex-1 flex-col items-center gap-1.5">
                    <span className="text-muted-foreground text-xs font-medium">
                      {dayLabels[day.day]}
                    </span>
                    <span
                      className={cn(
                        "flex size-9 items-center justify-center rounded-full text-[10px] font-bold",
                        statusStyles[day.status]
                      )}
                      title={statusLabels[day.status]}
                    >
                      {statusLabels[day.status].slice(0, 1)}
                    </span>
                  </div>
                ))}
              </div>
            )}
            {summaryQuery.data && (
              <p className="text-muted-foreground text-xs">
                {summaryQuery.data.office} ofiste · {summaryQuery.data.remote} uzaktan ·{" "}
                {summaryQuery.data.leave} izinli
              </p>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
