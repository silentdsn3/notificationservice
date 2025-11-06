package com.notificationservice.service;

import com.notificationservice.config.TriggerProvider;
import com.notificationservice.model.Trigger;
import com.notificationservice.service.pattern.PatternMatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Движок для проверки триггеров на основе паттернов активности
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TriggerEngine {

    private final TriggerProvider triggerProvider;
    private final PatternMatcher patternMatcher;

    /**
     * Проверяет все триггеры для указанного действия пользователя
     */
    public List<Trigger> checkTriggers(String userId, String actionType, Instant timestamp) {
        List<Trigger> relevantTriggers = triggerProvider.getTriggersByActionType(actionType);
        List<Trigger> matchedTriggers = new ArrayList<>();

        for (Trigger trigger : relevantTriggers) {
            try {
                if (patternMatcher.matches(trigger, userId, actionType, timestamp)) {
                    matchedTriggers.add(trigger);
                }
            } catch (Exception e) {
                log.error("Ошибка при проверке триггера {} для пользователя {}: {}",
                         trigger.getId(), userId, e.getMessage(), e);
                // Продолжаем проверку других триггеров при ошибке
            }
        }

        return matchedTriggers;
    }
}