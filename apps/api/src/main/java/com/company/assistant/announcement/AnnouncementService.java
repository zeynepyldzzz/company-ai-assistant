package com.company.assistant.announcement;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * C-9 (#53): GET /announcements, GET /announcements/{id}, GET /notifications,
 * PUT /notifications/preferences. FR-45-47, 65-67.
 */
@Service
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final NotificationPreferenceRepository preferenceRepository;

    public AnnouncementService(AnnouncementRepository announcementRepository,
                                NotificationPreferenceRepository preferenceRepository) {
        this.announcementRepository = announcementRepository;
        this.preferenceRepository = preferenceRepository;
    }

    /** FR-45-46: duyuru listesi, sabitlenenler ustte. */
    @Transactional(readOnly = true)
    public List<AnnouncementDto> getAnnouncements() {
        return announcementRepository.findAllByOrderByPinnedDescPublishedAtDesc().stream()
                .map(AnnouncementDto::from)
                .toList();
    }

    /** GET /announcements/active: ana sayfada gosterilecek, suresi dolmamis duyurular. */
    @Transactional(readOnly = true)
    public List<AnnouncementDto> getActiveAnnouncements() {
        return announcementRepository.findActiveOrderByPinnedDescPublishedAtDesc(LocalDateTime.now()).stream()
                .map(AnnouncementDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AnnouncementDto getAnnouncement(Integer id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new AnnouncementNotFoundException("Duyuru bulunamadı: " + id));
        return AnnouncementDto.from(announcement);
    }

    /**
     * GET /notifications: calisan announcement bildirimlerini kapatmissa (FR-65-67
     * tercihi) bos liste doner, acikken duyuru listesiyle aynidir.
     */
    @Transactional(readOnly = true)
    public List<AnnouncementDto> getNotifications(Integer employeeId) {
        boolean announcementEnabled = preferenceRepository.findByEmployeeId(employeeId)
                .map(NotificationPreference::isAnnouncementEnabled)
                .orElse(true);
        if (!announcementEnabled) {
            return List.of();
        }
        return getAnnouncements();
    }

    @Transactional(readOnly = true)
    public NotificationPreferenceResponse getPreferences(Integer employeeId) {
        return preferenceRepository.findByEmployeeId(employeeId)
                .map(NotificationPreferenceResponse::from)
                .orElseGet(() -> new NotificationPreferenceResponse(true, true, true));
    }

    /** FR-65-67: calisan bildirim tercihlerini gunceller (yoksa olusturur). */
    @Transactional
    public NotificationPreferenceResponse updatePreferences(Integer employeeId, NotificationPreferenceRequest request) {
        NotificationPreference preference = preferenceRepository.findByEmployeeId(employeeId)
                .orElseGet(() -> {
                    NotificationPreference created = new NotificationPreference();
                    created.setEmployeeId(employeeId);
                    return created;
                });
        preference.setAnnouncementEnabled(request.announcementEnabled());
        preference.setScheduleEnabled(request.scheduleEnabled());
        preference.setSurveyEnabled(request.surveyEnabled());
        preference.setUpdatedAt(LocalDateTime.now());
        return NotificationPreferenceResponse.from(preferenceRepository.save(preference));
    }
}
