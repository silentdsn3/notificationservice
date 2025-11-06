package com.notificationservice.config;

import com.notificationservice.model.Trigger;
import com.notificationservice.repository.TriggerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Провайдер триггеров с кэшированием для эффективного доступа
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TriggerProvider {

    private final TriggerRepository triggerRepository;
    private final Map<String, List<Trigger>> triggersByActionType = new ConcurrentHashMap<>();
    private volatile boolean initialized = false;

    /**
     * Получает триггеры для указанного типа действия
     */
    public List<Trigger> getTriggersByActionType(String actionType) {
        if (!initialized) {
            initializeTriggersCache();
        }
        return triggersByActionType.getOrDefault(actionType, Collections.emptyList());
    }

    private synchronized void initializeTriggersCache() {
        if (initialized) return;

        List<Trigger> allTriggers = triggerRepository.findAll();
        allTriggers.forEach(trigger ->
                triggersByActionType
                        .computeIfAbsent(trigger.getActionType(), k -> new java.util.ArrayList<>())
                        .add(trigger)
        );

        logRegisteredTriggers();
        initialized = true;
    }

    private void logRegisteredTriggers() {
        log.info("=== Зарегистрированные триггеры ===");
        triggersByActionType.forEach((actionType, triggers) -> {
            log.info("Тип действия: {}", actionType);
            triggers.forEach(trigger ->
                    log.info("  - {} ({}) - cooldown: {}ч",
                            trigger.getName(), trigger.getPatternType(), trigger.getCooldownHours())
            );
        });
        log.info("===================================");
    }
}