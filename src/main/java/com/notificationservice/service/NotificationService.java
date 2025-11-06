package com.notificationservice.service;

import com.notificationservice.model.Notification;
import com.notificationservice.model.Trigger;
import com.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Сервис управления уведомлениями
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * Проверяет, можно ли отправлять уведомление (учитывая cooldown)
     */
    public boolean shouldSendNotification(String userId, Trigger trigger) {
        Instant since = Instant.now().minus(trigger.getCooldownHours(), ChronoUnit.HOURS);
        List<Notification> recent = notificationRepository.findRecentByUserAndTrigger(
                userId, trigger.getId(), since);
        return recent.isEmpty();
    }

    /**
     * Создает новое уведомление
     */
    public void createNotification(String userId, Trigger trigger) {
        Notification notification = new Notification(
                null, userId, "Умное уведомление",
                trigger.getMessageTemplate(), Instant.now(), "PENDING", trigger.getId()
        );
        notificationRepository.save(notification);
    }

    /**
     * Получает все уведомления (для API)
     */
    public List<Notification> getAllNotifications() {
        return notificationRepository.findAllByOrderByTimestampDesc();
    }
}