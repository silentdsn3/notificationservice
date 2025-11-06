package com.notificationservice.service.pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.notificationservice.model.Action;
import com.notificationservice.model.TriggerPattern;
import com.notificationservice.repository.ActionRepository;
import com.notificationservice.util.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Паттерн для проверки активности по дням недели
 * Использует UTC для всех временных вычислений
 */
@Component
public class WeekdayPattern extends TriggerPattern {

    private static final Logger logger = LoggerFactory.getLogger(WeekdayPattern.class);
    private final ActionRepository actionRepository;

    public WeekdayPattern(ActionRepository actionRepository) {
        this.actionRepository = actionRepository;
    }

    @Override
    public boolean matches(String userId, String actionType, Instant timestamp, TriggerPatternContext context) {
        try {
            JsonNode config = context.getConfig();

            // Проверяем requiredDays
            if (!config.has("requiredDays")) {
                logger.warn("Отсутствует requiredDays в конфигурации для user {}", userId);
                return false;
            }

            JsonNode requiredDaysNode = config.get("requiredDays");
            Set<DayOfWeek> requiredDays = parseDayOfWeekArray(requiredDaysNode);
            if (requiredDays.isEmpty()) {
                logger.warn("Нет валидных required days в конфигурации для user {}", userId);
                return false;
            }

            // Проверяем checkDay в UTC
            String checkDayStr = config.has("checkDay") ? config.get("checkDay").asText() : null;
            DayOfWeek currentDay = TimeUtils.toUtcDayOfWeek(timestamp);

            if (checkDayStr != null && !checkDayStr.isEmpty()) {
                try {
                    DayOfWeek checkDay = parseDayOfWeek(checkDayStr);
                    if (currentDay != checkDay) {
                        return false;
                    }
                } catch (IllegalArgumentException e) {
                    logger.warn("Невалидный checkDay в конфигурации: {}", checkDayStr);
                    // Продолжаем выполнение, если checkDay некорректен
                }
            }

            // Получаем windowWeeks с значением по умолчанию
            int windowWeeks = 1;
            if (config.has("windowWeeks")) {
                windowWeeks = config.get("windowWeeks").asInt();
            }

            Instant startDate = TimeUtils.calculateWindowStart(timestamp, windowWeeks * 7L, ChronoUnit.DAYS);

            List<Action> recentActions = actionRepository.findByUserIdAndActionTypeAndTimestampAfterOrderByTimestampDesc(
                    userId, actionType, startDate);

            // Используем UTC для анализа дней недели
            Set<DayOfWeek> actionDays = recentActions.stream()
                    .map(action -> TimeUtils.toUtcDayOfWeek(action.getTimestamp()))
                    .collect(Collectors.toSet());

            return actionDays.containsAll(requiredDays);

        } catch (Exception e) {
            logger.error("Ошибка в WeekdayPattern для user {}: {}", userId, e.getMessage(), e);
            return false;
        }
    }

    private Set<DayOfWeek> parseDayOfWeekArray(JsonNode daysArrayNode) {
        List<DayOfWeek> days = new ArrayList<>();

        if (daysArrayNode.isArray()) {
            for (JsonNode dayNode : daysArrayNode) {
                try {
                    DayOfWeek day = parseDayOfWeek(dayNode.asText());
                    days.add(day);
                } catch (IllegalArgumentException e) {
                    logger.warn("Невалидный день недели: {}", dayNode.asText());
                }
            }
        }

        return Set.copyOf(days);
    }

    private DayOfWeek parseDayOfWeek(String dayStr) {
        return DayOfWeek.valueOf(dayStr.toUpperCase());
    }
}