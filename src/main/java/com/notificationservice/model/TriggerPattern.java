package com.notificationservice.model;

import com.notificationservice.service.pattern.TriggerPatternContext;

import java.time.Instant;

/**
 * Абстрактный базовый класс для всех паттернов триггеров
 */
public abstract class TriggerPattern {
    public abstract boolean matches(String userId, String actionType, Instant timestamp,
                                    TriggerPatternContext context);
}