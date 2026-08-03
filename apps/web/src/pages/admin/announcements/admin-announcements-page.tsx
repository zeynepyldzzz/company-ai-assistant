import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Pin, PinOff } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/auth/auth-context";
import { ApiError } from "@/api/client";
import { listAnnouncements, togglePinAnnouncement } from "@/api/announcement";
import { AnnouncementCreateSheet } from "./announcement-create-sheet";
import { AnnouncementEditSheet } from "./announcement-edit-sheet";

function formatDate(value: string): string {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString("tr-TR");
}

// C-9 (#53): Admin duyuru oluşturma + sabitleme (FR-45-47).
export function AdminAnnouncementsPage() {
  const { token } = useAuth();
  const queryClient = useQueryClient();

  const { data, isLoading, isError } = useQuery({
    queryKey: ["admin", "announcements"],
    queryFn: () => listAnnouncements(token!),
    enabled: Boolean(token),
  });

  const pinMutation = useMutation({
    mutationFn: (id: number) => togglePinAnnouncement(id, token!),
    onSuccess: () => {
      toast.success("Duyuru güncellendi.");
      queryClient.invalidateQueries({ queryKey: ["admin", "announcements"] });
      queryClient.invalidateQueries({ queryKey: ["announcements", "active"] });
    },
    onError: (error) => {
      const message = error instanceof ApiError ? error.message : "Duyuru güncellenemedi.";
      toast.error(message);
    },
  });

  return (
    <div className="space-y-4">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-xl font-semibold">Duyurular</h1>
          <p className="text-muted-foreground text-sm">
            Duyuru oluşturun, sabitleyin. Sabitlenenler listede üstte gösterilir.
          </p>
        </div>
        <AnnouncementCreateSheet />
      </div>

      {isLoading && <p className="text-muted-foreground text-sm">Yükleniyor…</p>}
      {isError && <p className="text-destructive text-sm">Duyurular yüklenemedi.</p>}

      {data && (
        <div className="overflow-x-auto rounded-lg border">
          <table className="w-full text-sm">
            <thead className="bg-muted/50">
              <tr>
                <th className="px-4 py-2 text-left font-medium">Başlık</th>
                <th className="px-4 py-2 text-center font-medium">Durum</th>
                <th className="px-4 py-2 text-left font-medium">Oluşturulma</th>
                <th className="px-4 py-2 text-left font-medium">Geçerlilik</th>
                <th className="px-4 py-2 text-right font-medium">İşlemler</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {data.length === 0 ? (
                <tr>
                  <td colSpan={5} className="text-muted-foreground px-4 py-3 text-center">
                    Henüz duyuru yok.
                  </td>
                </tr>
              ) : (
                data.map((announcement) => (
                  <tr key={announcement.id}>
                    <td className="px-4 py-2 font-medium">{announcement.title}</td>
                    <td className="px-4 py-2 text-center">
                      <span
                        className={
                          announcement.pinned
                            ? "rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-800"
                            : "bg-muted text-muted-foreground rounded-full px-2 py-0.5 text-xs font-medium"
                        }
                      >
                        {announcement.pinned ? "Sabitlendi" : "Normal"}
                      </span>
                    </td>
                    <td className="text-muted-foreground px-4 py-2">
                      {formatDate(announcement.publishedAt)}
                    </td>
                    <td className="px-4 py-2">
                      {announcement.expiresAt ? (
                        <span className={announcement.active ? "" : "text-destructive"}>
                          {formatDate(announcement.expiresAt)}
                          {!announcement.active && " (Süresi Doldu)"}
                        </span>
                      ) : (
                        <span className="text-muted-foreground">Süresiz</span>
                      )}
                    </td>
                    <td className="px-4 py-2">
                      <div className="flex justify-end gap-2">
                        <AnnouncementEditSheet announcement={announcement} />
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => pinMutation.mutate(announcement.id)}
                          disabled={pinMutation.isPending}
                        >
                          {announcement.pinned ? <PinOff /> : <Pin />}
                          {announcement.pinned ? "Sabitlemeyi Kaldır" : "Sabitle"}
                        </Button>
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
