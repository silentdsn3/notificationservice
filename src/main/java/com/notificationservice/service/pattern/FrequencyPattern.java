package com.notificationservice.service.pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.notificationservice.model.TriggerPattern;
import com.notificationservice.repository.ActionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Паттерн частоты действий - проверяет количество действий в временном окне
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FrequencyPattern extends TriggerPattern {

    private final ActionRepository actionRepository;

    @Override
    public boolean matches(String userId, String actionType, Instant timestamp, TriggerPatternContext context) {
        try {
            JsonNode config = context.getConfig();

            // Проверяем обязательные поля конфигурации
            if (!config.has("actionCount") || !config.has("timeWindowMinutes")) {
                log.warn("FrequencyPattern: отсутствуют обязательные поля конфигурации для пользователя {}", userId);
                return false;
            }

            int actionCount = config.get("actionCount").asInt();
            int timeWindowMinutes = config.get("timeWindowMinutes").asInt();

            // Подсчитываем действия во временном окне
            Instant windowStart = timestamp.minus(timeWindowMinutes, ChronoUnit.MINUTES);
            long count = actionRepository.countByUserAndActionTypeAndTimeWindow(
                    userId, actionType, windowStart, timestamp);

            boolean matches = count >= actionCount;

            if (matches) {
                log.debug("FrequencyPattern сработал для пользователя {}: {} действий за {} минут (требуется: {})",
                         userId, count, timeWindowMinutes, actionCount);
            }

            return matches;

        } catch (Exception e) {
            log.error("Ошибка при проверке FrequencyPattern для пользователя {}: {}", userId, e.getMessage(), e);
            return false;
        }
    }
}