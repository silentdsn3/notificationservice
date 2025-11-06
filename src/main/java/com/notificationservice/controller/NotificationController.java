package com.notificationservice.controller;

import com.notificationservice.dto.NotificationDto;
import com.notificationservice.model.Notification;
import com.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Контроллер для получения уведомлений
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationDto>> getNotifications() {
        List<Notification> notifications = notificationService.getAllNotifications();

        List<NotificationDto> dtos = notifications.stream()
                .map(this::convertToDto)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    private NotificationDto convertToDto(Notification notification) {
        return new NotificationDto(
                notification.getId(),
                notification.getUserId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getTimestamp(),
                notification.getStatus()
        );
    }
}