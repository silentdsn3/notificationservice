package com.notificationservice.service;

import com.notificationservice.config.ConfigurationValidator;
import com.notificationservice.dto.ActionDto;
import com.notificationservice.model.Action;
import com.notificationservice.model.Trigger;
import com.notificationservice.repository.ActionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Обработчик действий пользователя
 * Сохраняет действия и проверяет срабатывание триггеров
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ActionProcessor {

    private final ActionRepository actionRepository;
    private final TriggerEngine triggerEngine;
    private final NotificationService notificationService;
    private final ConfigurationValidator configurationValidator;

    @Transactional
    public void processAction(ActionDto actionDto) {
        try {
            // Сохраняем действие
            Action action = new Action(null, actionDto.getUserId(),
                    actionDto.getActionType(), actionDto.getTimestamp());
            actionRepository.save(action);
            log.debug("Action saved: user={}, type={}, time={}",
                     actionDto.getUserId(), actionDto.getActionType(), actionDto.getTimestamp());

            // Проверяем триггеры
            List<Trigger> triggered = triggerEngine.checkTriggers(
                    actionDto.getUserId(), actionDto.getActionType(), actionDto.getTimestamp());

            log.debug("Found {} triggered patterns for user {}", triggered.size(), actionDto.getUserId());

            // Создаем уведомления для сработавших триггеров (с валидацией)
            int notificationsCreated = 0;
            for (Trigger trigger : triggered) {
                if (configurationValidator.validateTriggerConfig(trigger) &&
                    notificationService.shouldSendNotification(actionDto.getUserId(), trigger)) {

                    notificationService.createNotification(actionDto.getUserId(), trigger);
                    notificationsCreated++;
                    log.info("Создано уведомление для user {} с триггером {}",
                            actionDto.getUserId(), trigger.getName());
                }
            }

            if (notificationsCreated > 0) {
                log.info("Created {} notifications for user {}", notificationsCreated, actionDto.getUserId());
            }

        } catch (Exception e) {
            log.error("Error processing action for user {}: {}", actionDto.getUserId(), e.getMessage(), e);
            throw e; // Перебрасываем исключение для корректной обработки транзакции
        }
    }
}