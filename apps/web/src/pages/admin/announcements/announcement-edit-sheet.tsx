import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Pencil } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
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
import { updateAnnouncement } from "@/api/announcement";
import { todayDateInputValue } from "@/lib/utils";
import type { Announcement } from "@company/shared";

// B-18: PUT /admin/announcements/{id} — baslik, icerik, gecerlilik tarihi duzenleme.
export function AnnouncementEditSheet({ announcement }: { announcement: Announcement }) {
  const { token } = useAuth();
  const queryClient = useQueryClient();

  const [open, setOpen] = useState(false);
  const [title, setTitle] = useState(announcement.title);
  const [content, setContent] = useState(announcement.content);
  const [expiresAt, setExpiresAt] = useState(announcement.expiresAt?.slice(0, 10) ?? "");

  const mutation = useMutation({
    mutationFn: () =>
      updateAnnouncement(
        announcement.id,
        {
          title: title.trim(),
          content: content.trim(),
          expiresAt: expiresAt ? `${expiresAt}T23:59:59` : null,
        },
        token!
      ),
    onSuccess: () => {
      toast.success("Duyuru güncellendi.");
      queryClient.invalidateQueries({ queryKey: ["admin", "announcements"] });
      queryClient.invalidateQueries({ queryKey: ["announcements", "active"] });
      setOpen(false);
    },
    onError: (error) => {
      const message = error instanceof ApiError ? error.message : "Duyuru güncellenemedi.";
      toast.error(message);
    },
  });

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    if (!title.trim() || !content.trim()) {
      toast.error("Başlık ve içerik boş olamaz.");
      return;
    }
    if (expiresAt && expiresAt < todayDateInputValue()) {
      toast.error("Geçerlilik tarihi geçmiş bir tarih olamaz.");
      return;
    }
    mutation.mutate();
  }

  function handleOpenChange(next: boolean) {
    if (next) {
      setTitle(announcement.title);
      setContent(announcement.content);
      setExpiresAt(announcement.expiresAt?.slice(0, 10) ?? "");
    }
    setOpen(next);
  }

  return (
    <Sheet open={open} onOpenChange={handleOpenChange}>
      <SheetTrigger
        render={
          <Button size="sm" variant="outline">
            <Pencil />
            Düzenle
          </Button>
        }
      />
      <SheetContent className="w-full overflow-y-auto sm:max-w-lg">
        <SheetHeader>
          <SheetTitle>Duyuruyu Düzenle</SheetTitle>
          <SheetDescription>Değişiklikler kaydedilince tüm çalışanların listesine yansır.</SheetDescription>
        </SheetHeader>

        <form onSubmit={handleSubmit} className="flex flex-1 flex-col gap-4 px-4">
          <div className="space-y-1.5">
            <Label htmlFor="announcement-edit-title">Başlık</Label>
            <Input
              id="announcement-edit-title"
              value={title}
              onChange={(event) => setTitle(event.target.value)}
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="announcement-edit-content">İçerik</Label>
            <Textarea
              id="announcement-edit-content"
              value={content}
              onChange={(event) => setContent(event.target.value)}
              rows={5}
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="announcement-edit-expires-at">Geçerlilik Tarihi (Bitiş)</Label>
            <Input
              id="announcement-edit-expires-at"
              type="date"
              min={todayDateInputValue()}
              value={expiresAt}
              onChange={(event) => setExpiresAt(event.target.value)}
            />
            <p className="text-muted-foreground text-xs">Boş bırakılırsa duyuru süresiz kalır.</p>
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
