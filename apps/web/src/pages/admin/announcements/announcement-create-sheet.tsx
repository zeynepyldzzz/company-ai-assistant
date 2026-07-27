import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Plus } from "lucide-react";
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
import { createAnnouncement } from "@/api/announcement";

// C-9 (#53): POST /admin/announcements.
export function AnnouncementCreateSheet() {
  const { token } = useAuth();
  const queryClient = useQueryClient();

  const [open, setOpen] = useState(false);
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");

  const mutation = useMutation({
    mutationFn: () => createAnnouncement({ title: title.trim(), content: content.trim() }, token!),
    onSuccess: () => {
      toast.success("Duyuru oluşturuldu.");
      queryClient.invalidateQueries({ queryKey: ["admin", "announcements"] });
      setTitle("");
      setContent("");
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
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              placeholder="Örn. Yaz Tatili Duyurusu"
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="announcement-content">İçerik</Label>
            <Textarea
              id="announcement-content"
              value={content}
              onChange={(event) => setContent(event.target.value)}
              rows={5}
            />
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
