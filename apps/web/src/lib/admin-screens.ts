import {
  CalendarDays,
  ClipboardList,
  Car,
  BookText,
  Users,
  KeyRound,
  ChefHat,
  Building2,
  Bus,
  type LucideIcon,
} from "lucide-react";
import type { AdminSubRole } from "@company/shared";

/**
 * A-36 (#200): yonetim ekranlarinin alt rol haritasi.
 *
 * <p>Degerler backend'deki @PreAuthorize guard'larindan TURETILDI, tahminle yazilmadi.
 * Buradaki liste guard'lardan genis olursa kullanici tiklayip 403 aliyor; dar olursa
 * erisebildigi bir ekrani hic goremiyor — ikincisi daha kotu, cunku kullanici ozelligin
 * var oldugunu bile bilmiyor. Guard degistiginde bu dosya da guncellenmeli.
 *
 * <p>system_admin her ekranda ayrica listeleniyor; guard'lar da oyle yaziliyor
 * ("X_ADMIN or SYSTEM_ADMIN"). Ozel durum olarak kod icinde ele almak yerine veriyi
 * guard'la birebir ayni tutmak, iki tarafi karsilastirmayi kolaylastiriyor.
 */
export interface AdminScreen {
  to: string;
  label: string;
  icon: LucideIcon;
  subRoles: readonly AdminSubRole[];
}

export const ADMIN_SCREENS: readonly AdminScreen[] = [
  {
    to: "/admin/schedules",
    label: "Çalışan Düzeni",
    icon: CalendarDays,
    subRoles: ["hr_admin", "system_admin"],
  },
  {
    to: "/admin/surveys",
    label: "Anketler",
    icon: ClipboardList,
    subRoles: ["hr_admin", "system_admin"],
  },
  {
    to: "/admin/knowledge-base",
    label: "Dokümanlar",
    icon: BookText,
    subRoles: ["hr_admin", "system_admin"],
  },
  {
    to: "/admin/vehicles",
    label: "Araç Yönetimi",
    icon: Car,
    subRoles: ["fleet_admin", "system_admin"],
  },
  {
    to: "/admin/employees",
    label: "Çalışan Yönetimi",
    icon: Users,
    subRoles: ["hr_admin", "system_admin"],
  },
  {
    to: "/admin/roles",
    label: "Rol / İzin Yönetimi",
    icon: KeyRound,
    subRoles: ["system_admin"],
  },
  {
    to: "/admin/menu",
    label: "Menü Yönetimi",
    icon: ChefHat,
    subRoles: ["canteen_admin", "system_admin"],
  },
  {
    to: "/admin/departments",
    label: "Departman Yönetimi",
    icon: Building2,
    subRoles: ["hr_admin", "system_admin"],
  },
  {
    to: "/admin/shuttle-routes",
    label: "Servis Güzergah Yönetimi",
    icon: Bus,
    subRoles: ["shuttle_admin", "system_admin"],
  },
];

export function screensFor(subRole: AdminSubRole | null | undefined): AdminScreen[] {
  if (!subRole) return [];
  return ADMIN_SCREENS.filter((screen) => screen.subRoles.includes(subRole));
}
