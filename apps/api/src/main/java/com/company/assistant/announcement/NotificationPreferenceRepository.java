package com.company.assistant.announcement;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Integer> {

    Optional<NotificationPreference> findByEmployeeId(Integer employeeId);
}
