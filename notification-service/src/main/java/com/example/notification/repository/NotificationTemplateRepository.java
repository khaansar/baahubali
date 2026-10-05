package com.example.notification.repository;

import com.example.notification.entity.Notification;
import com.example.notification.entity.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, String> {

    Optional<NotificationTemplate> findFirstByTemplateCodeAndChannelAndActiveTrueOrderByVersionDesc(
            String templateCode,
            Notification.Channel channel
    );
}
