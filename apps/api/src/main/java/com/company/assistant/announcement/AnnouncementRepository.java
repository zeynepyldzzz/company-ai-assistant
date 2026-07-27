package com.company.assistant.announcement;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnouncementRepository extends JpaRepository<Announcement, Integer> {

    /** FR-45-47: sabitlenenler ustte, sonra en yeni. */
    List<Announcement> findAllByOrderByPinnedDescPublishedAtDesc();
}
