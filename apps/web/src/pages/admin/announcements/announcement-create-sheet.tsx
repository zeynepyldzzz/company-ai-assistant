import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { FIELD_LIMITS } from "@/lib/field-limits";
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
import { createAnnouncement } from "@/api/announcement";
import { todayDateInputValue } from "@/lib/utils";

// C-9 (#53): POST /admin/announcements.
export function AnnouncementCreateSheet() {
  const { token } = useAuth();
  const queryClient = useQueryClient();

  const [open, setOpen] = useState(false);
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [expiresAt, setExpiresAt] = useState("");

  const mutation = useMutation({
    mutationFn: () =>
      createAnnouncement(
        {
          title: title.trim(),
          content: content.trim(),
          expiresAt: expiresAt ? `${expiresAt}T23:59:59` : null,
        },
        token!
      ),
    onSuccess: () => {
      toast.success("Duyuru oluşturuldu.");
      queryClient.invalidateQueries({ queryKey: ["admin", "announcements"] });
      queryClient.invalidateQueries({ queryKey: ["announcements", "active"] });
      setTitle("");
      setContent("");
      setExpiresAt("");
      setOpen(false);
    },
    onError: (error) => {
      const message = error instanceof ApiError ? error.message : "Duyuru oluşturulamadı.";
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

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      <SheetTrigger
        render={
          <Button>
            <Plus />
            Yeni Duyuru
          </Button>
        }
      />
      <SheetContent className="w-full overflow-y-auto sm:max-w-lg">
        <SheetHeader>
          <SheetTitle>Yeni Duyuru</SheetTitle>
          <SheetDescription>Duyuru tüm çalışanların listesinde hemen görünür.</SheetDescription>
        </SheetHeader>

        <form onSubmit={handleSubmit} className="flex flex-1 flex-col gap-4 px-4">
          <div className="space-y-1.5">
            <Label htmlFor="announcement-title">Başlık</Label>
            <Input
              id="announcement-title"
              maxLength={FIELD_LIMITS.announcementTitle}
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              placeholder="Örn. Yaz Tatili Duyurusu"
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="announcement-content">İçerik</Label>
            <Textarea
              id="announcement-content"
              maxLength={FIELD_LIMITS.announcementContent}
              value={content}
              onChange={(event) => setContent(event.target.value)}
              rows={5}
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="announcement-expires-at">Geçerlilik Tarihi (Bitiş)</Label>
            <Input
              id="announcement-expires-at"
              type="date"
              min={todayDateInputValue()}
              value={expiresAt}
              onChange={(event) => setExpiresAt(event.target.value)}
            />
            <p className="text-muted-foreground text-xs">Boş bırakılırsa duyuru süresiz kalır.</p>
          </div>

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
