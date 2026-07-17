import { NavLink } from "react-router";
import { LayoutDashboard, ShieldCheck } from "lucide-react";
import type { Role } from "@company/shared";
import { cn } from "@/lib/utils";
import { useAuth } from "@/auth/auth-context";

const navItems: Array<{
  to: string;
  label: string;
  icon: typeof LayoutDashboard;
  roles: readonly Role[];
}> = [
  { to: "/", label: "Ana Sayfa", icon: LayoutDashboard, roles: ["employee", "admin"] },
  { to: "/admin", label: "Yönetim", icon: ShieldCheck, roles: ["admin"] },
];

export function Sidebar() {
  const { user } = useAuth();

  return (
    <nav className="bg-sidebar text-sidebar-foreground flex h-full w-56 flex-col gap-1 border-r p-3">
      {navItems
        .filter((item) => user && item.roles.includes(user.role))
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
