import { useQuery } from "@tanstack/react-query";
import { Card, CardContent } from "@/components/ui/card";
import { useAuth } from "@/auth/auth-context";
import { listAnnouncements } from "@/api/announcement";

function formatDate(value: string): string {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString("tr-TR");
}

// B-20: calisanlar icin salt-okunur duyuru gecmisi (GET /announcements,
// aktif + suresi dolmus tumu, sabitlenenler ustte - backend zaten bu sirada donuyor).
export function AnnouncementsPage() {
  const { token } = useAuth();

  const { data, isLoading, isError } = useQuery({
    queryKey: ["announcements", "all"],
    queryFn: () => listAnnouncements(token!),
    enabled: Boolean(token),
  });

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-xl font-semibold">Duyurular</h1>
        <p className="text-muted-foreground text-sm">Şirket duyurularının tamamını buradan görüntüleyebilirsiniz..</p>
      </div>

      {isLoading && <p className="text-muted-foreground text-sm">Yükleniyor…</p>}
      {isError && <p className="text-destructive text-sm">Duyurular yüklenemedi.</p>}

      {data && data.length === 0 && (
        <p className="text-muted-foreground text-sm">Henüz duyuru yok.</p>
      )}

      {data && data.length > 0 && (
        <div className="space-y-3">
          {data.map((announcement) => (
            <Card key={announcement.id}>
              <CardContent className="space-y-1.5 py-1">
                <div className="flex items-center gap-2">
                  <span className="font-medium">{announcement.title}</span>
                  {announcement.pinned && (
                    <span className="rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-800">
                      Sabitlendi
                    </span>
                  )}
                </div>
                <p className="text-sm whitespace-pre-wrap">{announcement.content}</p>
                <p className="text-muted-foreground text-xs">
                  {formatDate(announcement.publishedAt)}
                </p>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
