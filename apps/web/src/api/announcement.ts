import {
  AnnouncementSchema,
  NotificationPreferenceSchema,
  type AdminAnnouncementCreateRequest,
  type AdminAnnouncementUpdateRequest,
  type Announcement,
  type NotificationPreference,
} from "@company/shared";
import { apiFetch } from "./client";

const ADMIN_BASE = "/admin/announcements";

// GET /admin/announcements icin ayri liste ucu yok; admin ekrani da
// calisanlarin gordugu /announcements listesini kullanir (sabitlenenler ustte).
export async function listAnnouncements(token: string): Promise<Announcement[]> {
  const data = await apiFetch<unknown[]>("/announcements", { token });
  return data.map((item) => AnnouncementSchema.parse(item));
}

// B-18: GET /announcements/active — ana sayfada gosterilecek, suresi
// dolmamis duyurular (pinned DESC, publishedAt DESC siralanmis gelir).
export async function getActiveAnnouncements(token: string): Promise<Announcement[]> {
  const data = await apiFetch<unknown[]>("/announcements/active", { token });
  return data.map((item) => AnnouncementSchema.parse(item));
}

// POST /admin/announcements — yeni duyuru olusturur (varsayilan pinned=false).
export async function createAnnouncement(
  body: AdminAnnouncementCreateRequest,
  token: string
): Promise<Announcement> {
  const data = await apiFetch<unknown>(ADMIN_BASE, {
    method: "POST",
    token,
    body: JSON.stringify(body),
  });
  return AnnouncementSchema.parse(data);
}

// PUT /admin/announcements/{id}/pin — sabitleme durumunu tersine cevirir.
export async function togglePinAnnouncement(id: number, token: string): Promise<Announcement> {
  const data = await apiFetch<unknown>(`${ADMIN_BASE}/${id}/pin`, {
    method: "PUT",
    token,
  });
  return AnnouncementSchema.parse(data);
}

// B-18: PUT /admin/announcements/{id} — baslik, icerik, gecerlilik tarihi gunceller.
export async function updateAnnouncement(
  id: number,
  body: AdminAnnouncementUpdateRequest,
  token: string
): Promise<Announcement> {
  const data = await apiFetch<unknown>(`${ADMIN_BASE}/${id}`, {
    method: "PUT",
    token,
    body: JSON.stringify(body),
  });
  return AnnouncementSchema.parse(data);
}

// GET /notifications/preferences — calisanin kendi bildirim tercihleri.
export async function getNotificationPreferences(token: string): Promise<NotificationPreference> {
  const data = await apiFetch<unknown>("/notifications/preferences", { token });
  return NotificationPreferenceSchema.parse(data);
}

// PUT /notifications/preferences — FR-65-67.
export async function updateNotificationPreferences(
  body: NotificationPreference,
  token: string
): Promise<NotificationPreference> {
  const data = await apiFetch<unknown>("/notifications/preferences", {
    method: "PUT",
    token,
    body: JSON.stringify(body),
  });
  return NotificationPreferenceSchema.parse(data);
}
