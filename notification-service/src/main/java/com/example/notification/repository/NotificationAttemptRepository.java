package com.example.notification.repository;

import com.example.notification.entity.NotificationAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationAttemptRepository extends JpaRepository<NotificationAttempt, String> {

    List<NotificationAttempt> findByNotificationIdOrderByAttemptNumberAsc(
            String notificationId
    );

    long countByNotificationId(String notificationId);
}
