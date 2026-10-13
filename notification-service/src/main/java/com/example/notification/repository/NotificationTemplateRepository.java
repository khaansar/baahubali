package com.example.notification.repository;

import com.example.notification.entity.Notification;
import com.example.notification.entity.NotificationTemplate;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, String> {

    @Cacheable(
            cacheNames = "notification:active-template",
            key = "#p0 + ':' + #p1.name()",
            unless = "#result == null"
    )
    Optional<NotificationTemplate> findFirstByTemplateCodeAndChannelAndActiveTrueOrderByVersionDesc(
            String templateCode,
            Notification.Channel channel
    );
}
