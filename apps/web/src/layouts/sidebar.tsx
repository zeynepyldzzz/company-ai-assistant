import { useEffect, useRef, useState } from "react";
import { NavLink } from "react-router";
import {
  LayoutDashboard,
  ShieldCheck,
  Users,
  Building2,
  Bus,
  CalendarDays,
  Car,
  Megaphone,
  UtensilsCrossed,
  BarChart3,
  ChevronsLeft,
  ChevronsRight,
} from "lucide-react";
import type { Role, AdminSubRole } from "@company/shared";
import { cn } from "@/lib/utils";
import { useAuth } from "@/auth/auth-context";

export const navItems: Array<{
  to: string;
  label: string;
  icon: typeof LayoutDashboard;
  roles: readonly Role[];
  // verilmisse alt rol de eslenmeli (or. bilgi tabani: hr_admin / system_admin)
  subRoles?: readonly AdminSubRole[];
}> = [
  { to: "/", label: "Ana Sayfa", icon: LayoutDashboard, roles: ["employee", "admin"] },
  {
    to: "/directory/employees",
    label: "Çalışanlar",
    icon: Users,
    roles: ["employee", "admin"],
  },
  {
    to: "/directory/departments",
    label: "Departmanlar",
    icon: Building2,
    roles: ["employee", "admin"],
  },
  {
    // C-1 (#17): menu/meal endpoint'leri sidebar'a eklenmemisti.
    to: "/menu",
    label: "Menü",
    icon: UtensilsCrossed,
    roles: ["employee", "admin"],
  },
  {
    to: "/my-schedule",
    label: "Haftalık Çalışma Düzenim",
    icon: CalendarDays,
    roles: ["employee", "admin"],
  },
  { to: "/shuttle", label: "Servisler", icon: Bus, roles: ["employee", "admin"] },
  { to: "/vehicles", label: "Araç Rezervasyonu", icon: Car, roles: ["employee", "admin"] },
  // B-20: calisanlar salt-okunur /announcements'a, adminler yonetim ekrani
  // /admin/announcements'a gider - roller ayrik oldugu icin tek NavLink karismaz.
  { to: "/announcements", label: "Duyurular", icon: Megaphone, roles: ["employee"] },
  { to: "/admin/announcements", label: "Duyurular", icon: Megaphone, roles: ["admin"] },
  {
    to: "/admin/reports",
    label: "Raporlar",
    icon: BarChart3,
    roles: ["admin"],
    subRoles: ["system_admin"],
  },
  { to: "/admin", label: "Yönetim", icon: ShieldCheck, roles: ["admin"] },
];

const MIN_WIDTH = 76;
const MAX_WIDTH = 380;
const COLLAPSE_THRESHOLD = 96;
const DEFAULT_WIDTH = 264;
const STORAGE_KEY = "yasarbilgi-sidebar-width";

function readStoredWidth(): number {
  if (typeof window === "undefined") return DEFAULT_WIDTH;
  const raw = window.localStorage.getItem(STORAGE_KEY);
  const parsed = raw ? Number(raw) : NaN;
  return Number.isFinite(parsed) ? Math.min(MAX_WIDTH, Math.max(MIN_WIDTH, parsed)) : DEFAULT_WIDTH;
}

export function Sidebar() {
  const { user } = useAuth();
  const [width, setWidth] = useState(readStoredWidth);
  const draggingRef = useRef(false);
  const collapsed = width <= MIN_WIDTH + 4;

  useEffect(() => {
    function handleMove(e: PointerEvent) {
      if (!draggingRef.current) return;
      const next = Math.min(MAX_WIDTH, Math.max(MIN_WIDTH, e.clientX));
      setWidth(next < COLLAPSE_THRESHOLD ? MIN_WIDTH : next);
    }
    function handleUp() {
      if (!draggingRef.current) return;
      draggingRef.current = false;
      document.body.style.cursor = "";
      document.body.style.userSelect = "";
      setWidth((w) => {
        window.localStorage.setItem(STORAGE_KEY, String(w));
        return w;
      });
    }
    window.addEventListener("pointermove", handleMove);
    window.addEventListener("pointerup", handleUp);
    return () => {
      window.removeEventListener("pointermove", handleMove);
      window.removeEventListener("pointerup", handleUp);
    };
  }, []);

  function startDrag(e: React.PointerEvent) {
    e.preventDefault();
    draggingRef.current = true;
    document.body.style.cursor = "col-resize";
    document.body.style.userSelect = "none";
  }

  function toggleCollapse() {
    setWidth((w) => {
      const next = w <= MIN_WIDTH + 4 ? DEFAULT_WIDTH : MIN_WIDTH;
      window.localStorage.setItem(STORAGE_KEY, String(next));
      return next;
    });
  }

  return (
    <nav
      style={{ width }}
      className="bg-sidebar text-sidebar-foreground relative flex h-full shrink-0 flex-col border-r"
    >
      <div className="flex h-[72px] items-center px-3">
        <img
          src="/logo-wordmark.png"
          alt="Yaşar Bilgi"
          className="h-9 max-w-full object-contain object-left"
        />
      </div>

      <div className="flex-1 space-y-1 overflow-y-auto px-2 pb-3">
        {navItems
          .filter(
            (item) =>
              user &&
              item.roles.includes(user.role) &&
              (!item.subRoles || (user.subRole !== null && item.subRoles.includes(user.subRole)))
          )
          .map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              end={to === "/" || to === "/admin"}
              title={collapsed ? label : undefined}
              className={({ isActive }) =>
                cn(
                  "flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors",
                  collapsed && "justify-center px-0",
                  isActive
                    ? "bg-primary-soft text-primary"
                    : "hover:bg-accent hover:text-accent-foreground text-sidebar-foreground"
                )
              }
            >
              <Icon className="size-4 shrink-0" />
              {!collapsed && <span className="truncate">{label}</span>}
            </NavLink>
          ))}
      </div>

      <button
        type="button"
        onClick={toggleCollapse}
        aria-label={collapsed ? "Kenar çubuğunu genişlet" : "Kenar çubuğunu daralt"}
        className="text-muted-foreground hover:bg-accent hover:text-accent-foreground m-2 flex items-center justify-center rounded-md p-2 transition-colors"
      >
        {collapsed ? <ChevronsRight className="size-4" /> : <ChevronsLeft className="size-4" />}
      </button>

      <div
        onPointerDown={startDrag}
        className="hover:bg-primary/40 active:bg-primary/60 absolute top-0 right-0 h-full w-1 cursor-col-resize"
      />
    </nav>
  );
}
