package com.mugunghwa.goodquestion.admin.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    long countByParentIdAndReadAtIsNull(UUID parentId);
}
