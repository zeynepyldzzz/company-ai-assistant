import { NavLink } from "react-router";
import {
  LayoutDashboard,
  ShieldCheck,
  Users,
  Building2,
  Phone,
  Bus,
  MapPin,
  CalendarDays,
  Car,
  BookText,
} from "lucide-react";
import type { Role, AdminSubRole } from "@company/shared";
import { cn } from "@/lib/utils";
import { useAuth } from "@/auth/auth-context";

const navItems: Array<{
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
    label: "Çalışan Rehberi",
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
    to: "/directory/phonebook",
    label: "Telefon Rehberi",
    icon: Phone,
    roles: ["employee", "admin"],
  },
  {
    to: "/my-schedule",
    label: "Haftalık Çalışma Düzenim",
    icon: CalendarDays,
    roles: ["employee", "admin"],
  },
  { to: "/shuttle/routes", label: "Servis Güzergahları", icon: Bus, roles: ["employee", "admin"] },
  {
    to: "/shuttle/recommendation",
    label: "Bana En Yakın Servis",
    icon: MapPin,
    roles: ["employee", "admin"],
  },
  { to: "/vehicles", label: "Araç Rezervasyonu", icon: Car, roles: ["employee", "admin"] },
  { to: "/admin/schedules", label: "Çalışan Düzeni", icon: CalendarDays, roles: ["admin"] },
  {
    to: "/admin/vehicles",
    label: "Araç Yönetimi",
    icon: Car,
    roles: ["admin"],
    subRoles: ["fleet_admin", "system_admin"],
  },
  {
    to: "/admin/knowledge-base",
    label: "Bilgi Tabanı",
    icon: BookText,
    roles: ["admin"],
    subRoles: ["hr_admin", "system_admin"],
  },
  { to: "/admin", label: "Yönetim", icon: ShieldCheck, roles: ["admin"] },
];

export function Sidebar() {
  const { user } = useAuth();

  return (
    <nav className="bg-sidebar text-sidebar-foreground flex h-full w-56 flex-col gap-1 border-r p-3">
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
            end={to === "/"}
            className={({ isActive }) =>
              cn(
                "hover:bg-sidebar-accent hover:text-sidebar-accent-foreground flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium transition-colors",
                isActive && "bg-sidebar-accent text-sidebar-accent-foreground"
              )
            }
          >
            <Icon className="size-4" />
            {label}
          </NavLink>
        ))}
    </nav>
  );
}
