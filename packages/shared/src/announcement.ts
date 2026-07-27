import { z } from "zod";

// C-9 (#53): GET /announcements, GET /announcements/{id}, GET /notifications,
// PUT /notifications/preferences, admin: POST /admin/announcements,
// PUT /admin/announcements/{id}/pin.

// GET /announcements, GET /announcements/{id}, GET /notifications, admin cevaplari.
export const AnnouncementSchema = z.object({
  id: z.number(),
  title: z.string(),
  content: z.string(),
  pinned: z.boolean(),
  publishedAt: z.string(),
});
export type Announcement = z.infer<typeof AnnouncementSchema>;

// POST /admin/announcements govdesi.
export const AdminAnnouncementCreateRequestSchema = z.object({
  title: z.string().min(1),
  content: z.string().min(1),
});
export type AdminAnnouncementCreateRequest = z.infer<typeof AdminAnnouncementCreateRequestSchema>;

// GET/PUT /notifications/preferences govde/cevabi. FR-65-67.
export const NotificationPreferenceSchema = z.object({
  announcementEnabled: z.boolean(),
  scheduleEnabled: z.boolean(),
  surveyEnabled: z.boolean(),
});
export type NotificationPreference = z.infer<typeof NotificationPreferenceSchema>;
