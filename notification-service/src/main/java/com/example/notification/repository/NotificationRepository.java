package com.example.notification.repository;

import com.example.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, String> {

    Optional<Notification> findByEventIdAndChannel(
            String eventId,
            Notification.Channel channel
    );

    List<Notification> findTop100ByStatusOrderByCreatedAtAsc(
            Notification.Status status
    );

    List<Notification> findTop50ByUserIdAndChannelOrderByCreatedAtDesc(
            String userId,
            Notification.Channel channel
    );

    long countByUserIdAndChannelAndReadAtIsNull(
            String userId,
            Notification.Channel channel
    );

    Optional<Notification> findByIdAndUserIdAndChannel(
            String id,
            String userId,
            Notification.Channel channel
    );
}
