package com.notificationservice.service.pattern;

import com.notificationservice.model.Trigger;

import java.time.Instant;

/**
 * Интерфейс для сопоставления паттернов активности
 */
public interface PatternMatcher {
    boolean matches(Trigger trigger, String userId, String actionType, Instant timestamp);
}