import { z } from "zod";

// C-9 (#53): GET /announcements, GET /announcements/{id}, GET /notifications,
// PUT /notifications/preferences, admin: POST /admin/announcements,
// PUT /admin/announcements/{id}/pin.

// GET /announcements, GET /announcements/{id}, GET /notifications, admin cevaplari.
// B-18: expiresAt (gecerlilik/bitis tarihi, null = suresiz) ve active (suresi
// dolmamis mi) alanlari eklendi.
export const AnnouncementSchema = z.object({
  id: z.number(),
  title: z.string(),
  content: z.string(),
  pinned: z.boolean(),
  publishedAt: z.string(),
  expiresAt: z.string().nullable(),
  active: z.boolean(),
});
export type Announcement = z.infer<typeof AnnouncementSchema>;

// POST /admin/announcements govdesi. expiresAt opsiyoneldir (null = suresiz).
export const AdminAnnouncementCreateRequestSchema = z.object({
  title: z.string().min(1),
  content: z.string().min(1),
  expiresAt: z.string().nullable(),
});
export type AdminAnnouncementCreateRequest = z.infer<typeof AdminAnnouncementCreateRequestSchema>;

// B-18: PUT /admin/announcements/{id} govdesi.
export const AdminAnnouncementUpdateRequestSchema = z.object({
  title: z.string().min(1),
  content: z.string().min(1),
  expiresAt: z.string().nullable(),
});
export type AdminAnnouncementUpdateRequest = z.infer<typeof AdminAnnouncementUpdateRequestSchema>;

// GET/PUT /notifications/preferences govde/cevabi. FR-65-67.
export const NotificationPreferenceSchema = z.object({
  announcementEnabled: z.boolean(),
  scheduleEnabled: z.boolean(),
  surveyEnabled: z.boolean(),
});
export type NotificationPreference = z.infer<typeof NotificationPreferenceSchema>;
