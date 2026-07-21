import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useAuth } from "@/auth/auth-context";
import { getTodayMenu, getWeeklyMenu } from "@/api/menu";
import type { Menu, MealItem } from "@company/shared";

type Tab = "today" | "weekly";

function formatDate(isoDate: string): string {
  return new Date(isoDate).toLocaleDateString("tr-TR", {
    weekday: "long",
    day: "numeric",
    month: "long",
  });
}

function MealItemRow({ item }: { item: MealItem }) {
  return (
    <li className="flex flex-col gap-0.5 px-4 py-3">
      <span className="text-sm font-medium">{item.name}</span>
      <span className="text-muted-foreground text-xs">
        {item.calories !== null ? `${item.calories} kcal` : "Kalori bilgisi yok"}
        {" · "}
        {item.allergens ? `Alerjen: ${item.allergens}` : "Alerjen bilgisi yok"}
      </span>
    </li>
  );
}

function MenuCard({ menu }: { menu: Menu }) {
  return (
    <div className="rounded-lg border">
      <div className="border-b px-4 py-2">
        <h2 className="text-sm font-semibold">{formatDate(menu.date)}</h2>
      </div>
      {menu.items.length === 0 ? (
        <p className="text-muted-foreground px-4 py-3 text-sm">Bu gün için menü girilmemiş.</p>
      ) : (
        <ul className="divide-y">
          {menu.items.map((item) => (
            <MealItemRow key={item.id} item={item} />
          ))}
        </ul>
      )}
    </div>
  );
}

export function MenuPage() {
  const { token } = useAuth();
  const [tab, setTab] = useState<Tab>("today");

  const todayQuery = useQuery({
    queryKey: ["menu", "today"],
    queryFn: () => getTodayMenu(token!),
    enabled: Boolean(token) && tab === "today",
  });

  const weeklyQuery = useQuery({
    queryKey: ["menu", "weekly"],
    queryFn: () => getWeeklyMenu(token!),
    enabled: Boolean(token) && tab === "weekly",
  });

  const active = tab === "today" ? todayQuery : weeklyQuery;

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold">Yemek Menüsü</h1>

      <div className="inline-flex rounded-lg border p-1">
        <button
          type="button"
          onClick={() => setTab("today")}
          className={`rounded-md px-3 py-1.5 text-sm transition-colors ${
            tab === "today" ? "bg-muted font-medium" : "text-muted-foreground hover:bg-muted/50"
          }`}
        >
          Bugün
        </button>
        <button
          type="button"
          onClick={() => setTab("weekly")}
          className={`rounded-md px-3 py-1.5 text-sm transition-colors ${
            tab === "weekly" ? "bg-muted font-medium" : "text-muted-foreground hover:bg-muted/50"
          }`}
        >
          Bu Hafta
        </button>
      </div>

      {active.isLoading && <p className="text-muted-foreground text-sm">Yükleniyor…</p>}
      {active.isError && <p className="text-destructive text-sm">Menü yüklenemedi.</p>}

      {tab === "today" && todayQuery.data && <MenuCard menu={todayQuery.data} />}

      {tab === "weekly" && weeklyQuery.data && (
        <div className="space-y-3">
          {weeklyQuery.data.length === 0 ? (
            <p className="text-muted-foreground text-sm">Bu hafta için menü bulunamadı.</p>
          ) : (
            weeklyQuery.data.map((menu) => <MenuCard key={menu.id} menu={menu} />)
          )}
        </div>
      )}
    </div>
  );
}