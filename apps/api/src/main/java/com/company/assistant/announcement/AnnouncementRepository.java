package com.company.assistant.announcement;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnnouncementRepository extends JpaRepository<Announcement, Integer> {

    /** FR-45-47: sabitlenenler ustte, sonra en yeni. */
    List<Announcement> findAllByOrderByPinnedDescPublishedAtDesc();

    /** expiresAt null veya simdiden ileriyse aktif sayilir; sabitlenenler ustte. */
    @Query("SELECT a FROM Announcement a WHERE a.expiresAt IS NULL OR a.expiresAt > :now "
            + "ORDER BY a.pinned DESC, a.publishedAt DESC")
    List<Announcement> findActiveOrderByPinnedDescPublishedAtDesc(@Param("now") LocalDateTime now);
}
