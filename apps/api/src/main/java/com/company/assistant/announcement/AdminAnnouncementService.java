package com.company.assistant.announcement;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * C-9 (#53): POST /admin/announcements, PUT /admin/announcements/{id}/pin.
 * FR-46-47: admin duyuru olusturur ve sabitler. /admin/** SecurityConfig'te
 * hasRole("ADMIN") ile korunuyor.
 */
@Service
public class AdminAnnouncementService {

    private final AnnouncementRepository announcementRepository;

    public AdminAnnouncementService(AnnouncementRepository announcementRepository) {
        this.announcementRepository = announcementRepository;
    }

    /** POST /admin/announcements: yeni duyuru olusturur (varsayilan pinned=false). */
    @Transactional
    public AnnouncementDto create(Integer adminEmployeeId, AdminAnnouncementCreateRequest request) {
        if (request == null || request.title() == null || request.title().isBlank()) {
            throw new IllegalArgumentException("Duyuru başlığı boş olamaz");
        }
        if (request.content() == null || request.content().isBlank()) {
            throw new IllegalArgumentException("Duyuru içeriği boş olamaz");
        }

        Announcement announcement = new Announcement();
        announcement.setTitle(request.title());
        announcement.setContent(request.content());
        announcement.setPublishedBy(adminEmployeeId);
        announcement.setPublishedAt(LocalDateTime.now());
        announcement.setPinned(false);
        announcement.setExpiresAt(request.expiresAt());

        return AnnouncementDto.from(announcementRepository.save(announcement));
    }

    /** PUT /admin/announcements/{id}/pin: sabitleme durumunu tersine cevirir. */
    @Transactional
    public AnnouncementDto togglePin(Integer id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new AnnouncementNotFoundException("Duyuru bulunamadı: " + id));
        announcement.setPinned(!announcement.isPinned());
        return AnnouncementDto.from(announcementRepository.save(announcement));
    }

    /** PUT /admin/announcements/{id}: baslik, icerik ve gecerlilik tarihini gunceller. */
    @Transactional
    public AnnouncementDto update(Integer id, AdminAnnouncementUpdateRequest request) {
        if (request == null || request.title() == null || request.title().isBlank()) {
            throw new IllegalArgumentException("Duyuru başlığı boş olamaz");
        }
        if (request.content() == null || request.content().isBlank()) {
            throw new IllegalArgumentException("Duyuru içeriği boş olamaz");
        }

        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new AnnouncementNotFoundException("Duyuru bulunamadı: " + id));
        announcement.setTitle(request.title());
        announcement.setContent(request.content());
        announcement.setExpiresAt(request.expiresAt());
        return AnnouncementDto.from(announcementRepository.save(announcement));
    }

    /** B-21: DELETE /admin/announcements/{id}: duyuruyu siler. */
    @Transactional
    public void delete(Integer id) {
        if (!announcementRepository.existsById(id)) {
            throw new AnnouncementNotFoundException("Duyuru bulunamadı: " + id);
        }
        announcementRepository.deleteById(id);
    }
}
