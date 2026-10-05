package com.example.notification.worker;

import com.example.notification.entity.Notification;
import com.example.notification.repository.NotificationRepository;
import com.example.notification.service.NotificationOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationRetryWorker {

    private final NotificationRepository notificationRepository;
    private final NotificationOrchestrator notificationOrchestrator;

    @Scheduled(fixedDelayString = "${notification.retry-delay-seconds:30}000")
    public void retryFailedNotifications() {
        for (Notification notification :
                notificationRepository.findTop100ByStatusOrderByCreatedAtAsc(
                        Notification.Status.FAILED
                )) {
            try {
                notificationOrchestrator.deliver(notification);
            } catch (Exception e) {
                log.error(
                        "Failed to retry notification id={}",
                        notification.getId(),
                        e
                );
            }
        }
    }
}
