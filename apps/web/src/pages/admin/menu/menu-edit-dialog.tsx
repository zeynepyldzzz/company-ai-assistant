import { useEffect, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Pencil, Plus, Trash2 } from "lucide-react";
import type { Menu, MealItemRequest } from "@company/shared";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { useAuth } from "@/auth/auth-context";
import { ApiError } from "@/api/client";
import { updateMenu } from "@/api/menu";
import { CATEGORY_LABELS } from "./category-labels";

type MealItemDraft = { category: string; name: string };

const FIRST_CATEGORY = Object.keys(CATEGORY_LABELS)[0];

function toDrafts(menu: Menu): MealItemDraft[] {
  return menu.items.map((item) => ({ category: item.category ?? FIRST_CATEGORY, name: item.name }));
}

function toMealItemRequests(items: MealItemDraft[]): MealItemRequest[] {
  return items
    .filter((item) => item.name.trim())
    .map((item) => ({ category: item.category, name: item.name.trim() }));
}

function draftsEqual(a: MealItemDraft[], b: MealItemDraft[]): boolean {
  const normalize = (items: MealItemDraft[]) =>
    JSON.stringify(items.map((item) => ({ category: item.category, name: item.name.trim() })));
  return normalize(a) === normalize(b);
}

function formatDate(isoDate: string): string {
  return new Date(isoDate).toLocaleDateString("tr-TR", {
    weekday: "long",
    day: "numeric",
    month: "long",
  });
}

// #194: ayin tum gunleri tek modalda birden duzenlenebilir (sadece o hafta degil),
// tek "Kaydet" degisen gunleri PUT eder. category zaten monthlyMenu sorgusuyla
// geldigi icin (bkz. MealItemSchema) ayrica bir GET gerekmiyor.
export function MonthlyMenuEditDialog({ monthlyMenu }: { monthlyMenu: Menu[] }) {
  const { token } = useAuth();
  const queryClient = useQueryClient();

  const [open, setOpen] = useState(false);
  const [drafts, setDrafts] = useState<Record<number, MealItemDraft[]>>({});

  useEffect(() => {
    if (!open) return;
    setDrafts(Object.fromEntries(monthlyMenu.map((menu) => [menu.id, toDrafts(menu)])));
  }, [open, monthlyMenu]);

  function updateItem(menuId: number, index: number, patch: Partial<MealItemDraft>) {
    setDrafts((prev) => ({
      ...prev,
      [menuId]: prev[menuId].map((item, i) => (i === index ? { ...item, ...patch } : item)),
    }));
  }

  function removeItem(menuId: number, index: number) {
    setDrafts((prev) => ({ ...prev, [menuId]: prev[menuId].filter((_, i) => i !== index) }));
  }

  function addItem(menuId: number) {
    setDrafts((prev) => ({
      ...prev,
      [menuId]: [...prev[menuId], { category: FIRST_CATEGORY, name: "" }],
    }));
  }

  function dirtyMenus(): Menu[] {
    return monthlyMenu.filter((menu) => drafts[menu.id] && !draftsEqual(drafts[menu.id], toDrafts(menu)));
  }

  const mutation = useMutation({
    mutationFn: async () => {
      const changed = dirtyMenus();
      return Promise.all(
        changed.map((menu) => updateMenu(menu.id, { items: toMealItemRequests(drafts[menu.id]) }, token!))
      );
    },
    onSuccess: (results) => {
      toast.success(`${results.length} gün güncellendi.`);
      queryClient.invalidateQueries({ queryKey: ["menu"] });
      setOpen(false);
    },
    onError: (error) => {
      const message = error instanceof ApiError ? error.message : "Menü güncellenemedi.";
      toast.error(message);
    },
  });

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    const changed = dirtyMenus();
    if (changed.length === 0) {
      toast.error("Değişiklik yok.");
      return;
    }
    for (const menu of changed) {
      const cleaned = toMealItemRequests(drafts[menu.id]);
      if (cleaned.length === 0) {
        toast.error(`${formatDate(menu.date)}: en az bir kalem kalmalı (günü tamamen kaldırmak için Sil'i kullanın).`);
        return;
      }
    }
    mutation.mutate();
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger
        render={
          <Button variant="outline" size="sm">
            <Pencil />
            Düzenle
          </Button>
        }
      />
      <DialogContent className="w-full overflow-y-auto sm:max-w-6xl">
        <DialogHeader>
          <DialogTitle>Aylık Menüyü Düzenle</DialogTitle>
          <DialogDescription>Bir günün kalemlerini ekleyin, silin veya isimlerini değiştirin.</DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="flex flex-1 flex-col gap-4 px-4">
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {monthlyMenu.map((menu) => (
              <div key={menu.id} className="space-y-2 rounded-lg border p-3">
                <div className="flex items-center justify-between">
                  <p className="text-sm font-medium">{formatDate(menu.date)}</p>
                  <Button type="button" variant="ghost" size="icon-sm" onClick={() => addItem(menu.id)}>
                    <Plus />
                  </Button>
                </div>

                <div className="space-y-1.5">
                  {(drafts[menu.id] ?? []).map((item, index) => (
                    <div key={index} className="flex items-center gap-1.5">
                      <Select
                        value={item.category}
                        onValueChange={(value) => updateItem(menu.id, index, { category: value ?? item.category })}
                      >
                        <SelectTrigger className="w-32 shrink-0">
                          <SelectValue>
                            {(value: string | null) => (value ? (CATEGORY_LABELS[value] ?? value) : "Kategori")}
                          </SelectValue>
                        </SelectTrigger>
                        <SelectContent>
                          {Object.entries(CATEGORY_LABELS).map(([value, label]) => (
                            <SelectItem key={value} value={value}>
                              {label}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                      <Input
                        value={item.name}
                        onChange={(event) => updateItem(menu.id, index, { name: event.target.value })}
                        placeholder="Yemek adı"
                      />
                      <Button
                        type="button"
                        variant="ghost"
                        size="icon-sm"
                        onClick={() => removeItem(menu.id, index)}
                      >
                        <Trash2 />
                      </Button>
                    </div>
                  ))}
                  {(drafts[menu.id] ?? []).length === 0 && (
                    <p className="text-muted-foreground text-xs">Kalem yok.</p>
                  )}
                </div>
              </div>
            ))}
          </div>

          <DialogFooter className="px-0">
            <Button type="submit" disabled={mutation.isPending}>
              {mutation.isPending ? "Kaydediliyor…" : "Kaydet"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
