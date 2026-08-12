import { useEffect } from "react";
import { useLocation, useNavigate } from "react-router";
import { KeyRound, LogOut } from "lucide-react";
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

/**
 * A-44 (#219): sayfa basliginin SAHIBI bu serittir.
 *
 * Sayfalar kendi <h1>'ini yalnizca seridin soyleyemeyecegi bir sey varsa basar:
 *   - dinamik ad      -> /directory/departments/:id departman adini gosteriyor
 *   - farkli cerceve  -> ana sayfa "Hoş geldin, {ad}" diyor
 *   - alt sayfa       -> /admin/employees "Çalışan Yönetimi"; serit yalnizca "Yönetim" diyebilir
 *
 * Oncesinde sekiz sayfa kendi basligini da basiyordu ve ayni metin ekranda iki kez
 * gorunuyordu ("Departmanlar" / "Departmanlar").
 *
 * Bilinmeyen rotada UYGULAMA ADI donulmuyor: eskiden "Yaşar Bilgi Ofis Asistanı" doniyordu ve
 * /change-password gibi sayfalarda seritte sayfa basligi yerine uygulama adi yaziyordu. Bos
 * donmek dogru — o sayfalarin kendi basligi zaten var.
 */
function pageTitleFor(pathname: string): string {
  const exact = navItems.find((item) => item.to === pathname);
  if (exact) return exact.label;
  const nested = navItems
    .filter((item) => item.to !== "/" && pathname.startsWith(`${item.to}/`))
    .sort((a, b) => b.to.length - a.to.length)[0];
  return nested?.label ?? "";
}

/** Sekme basliginda her zaman gorunen uygulama adi. */
const APP_NAME = "Yaşar Bilgi Asistan";

export function Header() {
  const { user, clearAuth } = useAuth();
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const pageTitle = pageTitleFor(pathname);

  /**
   * A-44 (#219): sekme basligi tum sayfalarda sabit "Yaşar Bilgi Asistan" idi; kullanici
   * birden fazla sekme actiginda hangisinin hangi sayfa oldugunu ayirt edemiyordu.
   *
   * <p>Efekt Header'da: baslik mantigi zaten burada ve AppLayout'taki her sayfa bu bileseni
   * mount ediyor. /login ve /change-password AppLayout DISINDA (A-29 karari) — orada sekmede
   * yalnizca uygulama adinin yazmasi zaten dogru.
   *
   * <p>Temizlik, Header unmount oldugunda (o iki sayfaya gecerken) basligi uygulama adina
   * geri aliyor; aksi halde onceki sayfanin adi sekmede takili kalirdi.
   */
  useEffect(() => {
    document.title = pageTitle ? `${pageTitle} · ${APP_NAME}` : APP_NAME;
    return () => {
      document.title = APP_NAME;
    };
  }, [pageTitle]);

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
        <h1 className="min-w-0 flex-1 truncate pr-4 text-[19px] font-bold">{pageTitle}</h1>

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
              {/* A-29: kullanicinin kendi istegiyle sifre degistirmesi. Zorunlu akista
                  RequireAuth zaten buraya yonlendiriyor; bu, istege bagli giris noktasi. */}
              <DropdownMenuItem onClick={() => navigate("/change-password")}>
                <KeyRound className="size-4" />
                Şifre değiştir
              </DropdownMenuItem>
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
