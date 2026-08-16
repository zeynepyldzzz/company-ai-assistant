import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useAuth } from "@/auth/auth-context";
import { getTodayMenu, getWeeklyMenu, getMonthlyMenu } from "@/api/menu";
import type { Menu, MealItem } from "@company/shared";
import { isNotFound } from "@/api/client";

type Tab = "today" | "weekly" | "monthly";

function formatDate(isoDate: string): string {
  return new Date(isoDate).toLocaleDateString("tr-TR", {
    weekday: "long",
    day: "numeric",
    month: "long",
  });
}

function formatDayShort(isoDate: string): string {
  return new Date(isoDate).toLocaleDateString("tr-TR", { weekday: "short" });
}

function formatDayNumber(isoDate: string): string {
  return new Date(isoDate).toLocaleDateString("tr-TR", { day: "numeric", month: "short" });
}

function isToday(isoDate: string): boolean {
  const today = new Date();
  const date = new Date(isoDate);
  return (
    today.getFullYear() === date.getFullYear() &&
    today.getMonth() === date.getMonth() &&
    today.getDate() === date.getDate()
  );
}

// Yemekhane Excel'inde kalori/alerjen bilgisi hic gelmiyor (bkz. MenuExcelParser),
// bu yuzden "Kalori bilgisi yok" gibi hep ayni gorunen bos satiri gostermiyoruz;
// veri varsa (ileride eklenirse) yine gosteririz.
function MealItemRow({ item }: { item: MealItem }) {
  const hasExtraInfo = item.calories !== null || Boolean(item.allergens);
  return (
    <li className="flex flex-col gap-0.5 px-4 py-3">
      <span className="text-sm font-medium">{item.name}</span>
      {hasExtraInfo && (
        <span className="text-muted-foreground text-xs">
          {item.calories !== null && `${item.calories} kcal`}
          {item.calories !== null && item.allergens && " · "}
          {item.allergens && `Alerjen: ${item.allergens}`}
        </span>
      )}
    </li>
  );
}

function MenuCard({ menu }: { menu: Menu }) {
  return (
    <Card className="gap-0 p-0">
      <CardHeader className="border-b px-4 py-2">
        <CardTitle>{formatDate(menu.date)}</CardTitle>
      </CardHeader>
      <CardContent className="p-0">
        {menu.items.length === 0 ? (
          <p className="text-muted-foreground px-4 py-3 text-sm">Bu gün için menü girilmemiş.</p>
        ) : (
          <ul className="divide-y">
            {menu.items.map((item) => (
              <MealItemRow key={item.id} item={item} />
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  );
}

// Haftalik gorunum icin takvim benzeri grid: her gun bir sutun.
function WeeklyCalendar({ menus }: { menus: Menu[] }) {
  return (
    <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-5">
      {menus.map((menu) => (
        <Card
          key={menu.id}
          className={`gap-0 p-0 ${isToday(menu.date) ? "border-primary ring-primary/20 ring-1" : ""}`}
        >
          <CardHeader
            className={`border-b px-3 py-2 ${isToday(menu.date) ? "bg-primary/5" : "bg-muted/30"}`}
          >
            <CardTitle className="flex items-baseline justify-between text-sm">
              <span className="capitalize">{formatDayShort(menu.date)}</span>
              <span className="text-muted-foreground font-normal">{formatDayNumber(menu.date)}</span>
            </CardTitle>
          </CardHeader>
          <CardContent className="p-0">
            {menu.items.length === 0 ? (
              <p className="text-muted-foreground px-3 py-3 text-xs">Menü girilmemiş.</p>
            ) : (
              <ul className="divide-y">
                {menu.items.map((item) => (
                  <li key={item.id} className="px-3 py-2">
                    <span className="text-xs leading-snug font-medium">{item.name}</span>
                  </li>
                ))}
              </ul>
            )}
          </CardContent>
        </Card>
      ))}
    </div>
  );
}

const WEEKDAY_LABELS = ["Pzt", "Sal", "Çar", "Per", "Cum"];

function toISODate(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

// Ayin haftalara bolunmus Pzt-Cum izgarasi (hafta sonlari gosterilmiyor - yemekhane
// yalnizca is gunlerinde menu giriyor, WeeklyCalendar ile ayni varsayim). Ayin ilk
// haftasindaki ay-disi gunler ile son haftasindaki ay-disi gunler bos hucre olarak
// birakilir, boylece gercek bir takvim gorunumu olusur.
function getMonthWeeks(year: number, month: number): Date[][] {
  const firstOfMonth = new Date(year, month, 1);
  const lastOfMonth = new Date(year, month + 1, 0);
  const mondayOffset = (firstOfMonth.getDay() + 6) % 7;
  const cursor = new Date(firstOfMonth);
  cursor.setDate(cursor.getDate() - mondayOffset);

  const weeks: Date[][] = [];
  while (cursor <= lastOfMonth) {
    const week: Date[] = [];
    for (let i = 0; i < 5; i++) {
      week.push(new Date(cursor));
      cursor.setDate(cursor.getDate() + 1);
    }
    cursor.setDate(cursor.getDate() + 2);
    weeks.push(week);
  }
  return weeks;
}

function MonthlyCalendar({ menus }: { menus: Menu[] }) {
  const now = new Date();
  const menuByDate = new Map(menus.map((menu) => [menu.date, menu]));
  const weeks = getMonthWeeks(now.getFullYear(), now.getMonth());
  const monthLabel = now.toLocaleDateString("tr-TR", { month: "long", year: "numeric" });

  return (
    <div className="space-y-2">
      <p className="text-sm font-medium capitalize">{monthLabel}</p>
      <div className="overflow-x-auto">
        <div className="grid min-w-[640px] grid-cols-5 gap-2">
          {WEEKDAY_LABELS.map((label) => (
            <div key={label} className="text-muted-foreground px-1 text-xs font-medium">
              {label}
            </div>
          ))}
          {weeks.map((week) =>
            week.map((date) => {
              const iso = toISODate(date);
              if (date.getMonth() !== now.getMonth()) {
                return <div key={iso} />;
              }
              const menu = menuByDate.get(iso);
              return (
                <Card
                  key={iso}
                  className={`gap-0 p-0 ${isToday(iso) ? "border-primary ring-primary/20 ring-1" : ""}`}
                >
                  <CardHeader
                    className={`border-b px-2 py-1 ${isToday(iso) ? "bg-primary/5" : "bg-muted/30"}`}
                  >
                    <CardTitle className="text-xs font-normal">{date.getDate()}</CardTitle>
                  </CardHeader>
                  <CardContent className="p-0">
                    {menu && menu.items.length > 0 ? (
                      <ul className="divide-y">
                        {menu.items.map((item) => (
                          <li key={item.id} className="px-2 py-1">
                            <span className="text-xs leading-snug font-medium">{item.name}</span>
                          </li>
                        ))}
                      </ul>
                    ) : (
                      <p className="text-muted-foreground px-2 py-2 text-xs">—</p>
                    )}
                  </CardContent>
                </Card>
              );
            })
          )}
        </div>
      </div>
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

  const monthlyQuery = useQuery({
    queryKey: ["menu", "monthly"],
    queryFn: () => getMonthlyMenu(token!),
    enabled: Boolean(token) && tab === "monthly",
  });

  const active = tab === "today" ? todayQuery : tab === "weekly" ? weeklyQuery : monthlyQuery;

  /**
   * A-47 (#221): "bugün için menü yok" bir HATA değil, beklenen durum — yemekhane yalnızca iş
   * günlerinde menü giriyor ve hafta sonu ekranda "Menü yüklenemedi" yazıyordu.
   *
   * Backend 404 dönmeye devam ediyor; var olmayan bir kayıt için doğru yanıt o. Değişen şey,
   * arayüzün 404'ü hata değil BOŞ DURUM olarak göstermesi. Diğer hatalar (ağ, 500) hâlâ hata
   * olarak görünüyor.
   */
  const todayMenuMissing = tab === "today" && isNotFound(todayQuery.error);

  return (
    <div className="space-y-4">
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
        <button
          type="button"
          onClick={() => setTab("monthly")}
          className={`rounded-md px-3 py-1.5 text-sm transition-colors ${
            tab === "monthly" ? "bg-muted font-medium" : "text-muted-foreground hover:bg-muted/50"
          }`}
        >
          Bu Ay
        </button>
      </div>

      {active.isLoading && <p className="text-muted-foreground text-sm">Yükleniyor…</p>}
      {active.isError && !todayMenuMissing && (
        <p className="text-destructive text-sm">Menü yüklenemedi.</p>
      )}

      {/* A-47 (#221): menü kaydı YOKSA bu bir hata degil. Yemekhane yalnizca is gunlerinde
          menu giriyor; hafta sonu ekranda "Menü yüklenemedi" yaziyordu. */}
      {todayMenuMissing && (
        <p className="text-muted-foreground text-sm">
          Bugün için menü girilmemiş. Yemekhane yalnızca iş günlerinde menü yayınlıyor.
        </p>
      )}

      {tab === "today" && todayQuery.data && <MenuCard menu={todayQuery.data} />}

      {tab === "weekly" && weeklyQuery.data && (
        <>
          {weeklyQuery.data.length === 0 ? (
            <p className="text-muted-foreground text-sm">Bu hafta için menü bulunamadı.</p>
          ) : (
            <WeeklyCalendar menus={weeklyQuery.data} />
          )}
        </>
      )}

      {tab === "monthly" && monthlyQuery.data && <MonthlyCalendar menus={monthlyQuery.data} />}
    </div>
  );
}