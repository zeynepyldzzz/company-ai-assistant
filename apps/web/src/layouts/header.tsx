import { useLocation } from "react-router";
import { LogOut } from "lucide-react";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { buttonVariants } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useAuth } from "@/auth/auth-context";
import { readAuth } from "@/auth/token-store";
import { logout } from "@/api/auth";
import { ThemeToggle } from "@/components/theme-toggle";
import { FloatingChatBubble } from "@/components/floating-chat-bubble";
import { navItems } from "./sidebar";
import type { AdminSubRole } from "@company/shared";

const subRoleLabels: Record<AdminSubRole, string> = {
  system_admin: "Sistem Yöneticisi",
  hr_admin: "İK Yöneticisi",
  fleet_admin: "Filo Yöneticisi",
  shuttle_admin: "Servis Yöneticisi",
  canteen_admin: "Kantin Yöneticisi",
};

function pageTitleFor(pathname: string): string {
  const exact = navItems.find((item) => item.to === pathname);
  if (exact) return exact.label;
  const nested = navItems
    .filter((item) => item.to !== "/" && pathname.startsWith(`${item.to}/`))
    .sort((a, b) => b.to.length - a.to.length)[0];
  return nested?.label ?? "Yaşar Bilgi Ofis Asistanı";
}

export function Header() {
  const { user, clearAuth } = useAuth();
  const { pathname } = useLocation();

  async function handleLogout() {
    const refreshToken = readAuth()?.refreshToken;
    clearAuth();
    // Local oturum hemen kapanir; sunucudaki refresh token'i iptal etmek
    // best-effort'tur, basarisiz olsa da kullaniciyi bloke etmez.
    if (refreshToken) {
      logout(refreshToken).catch(() => {});
    }
  }

  const initials = user
    ? user.name
        .split(" ")
        .map((part) => part[0])
        .join("")
        .slice(0, 2)
        .toUpperCase()
    : "?";

  const roleLabel = user
    ? user.role === "admin" && user.subRole
      ? subRoleLabels[user.subRole]
      : "Çalışan"
    : "";

  return (
    <>
      <header className="flex h-14 shrink-0 items-center justify-between border-b px-4 md:px-6">
        <h1 className="min-w-0 flex-1 truncate pr-4 text-[19px] font-bold">
          {pageTitleFor(pathname)}
        </h1>

        <div className="flex shrink-0 items-center gap-2">
          <ThemeToggle />
          <DropdownMenu>
            <DropdownMenuTrigger
              className={buttonVariants({
                variant: "ghost",
                className: "flex items-center gap-2 px-2",
              })}
            >
              <Avatar className="size-7">
                <AvatarFallback className="bg-primary-soft text-primary">{initials}</AvatarFallback>
              </Avatar>
              <span className="flex flex-col items-start leading-tight">
                <span className="text-sm font-medium">{user?.name ?? "Kullanıcı"}</span>
                <span className="text-muted-foreground text-xs">{roleLabel}</span>
              </span>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem onClick={handleLogout}>
                <LogOut className="size-4" />
                Çıkış yap
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </header>
      <FloatingChatBubble />
    </>
  );
}
