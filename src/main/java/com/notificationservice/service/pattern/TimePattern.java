package com.notificationservice.service.pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.notificationservice.model.Action;
import com.notificationservice.model.TriggerPattern;
import com.notificationservice.repository.ActionRepository;
import com.notificationservice.util.TimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Паттерн времени суток - проверяет активность в определенные часы
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TimePattern extends TriggerPattern {

    private final ActionRepository actionRepository;

    @Override
    public boolean matches(String userId, String actionType, Instant timestamp, TriggerPatternContext context) {
        try {
            JsonNode config = context.getConfig();

            // Парсим конфигурацию временного окна
            LocalTime startTime = TimeUtils.parseTime(config.get("startTime").asText());
            LocalTime endTime = TimeUtils.parseTime(config.get("endTime").asText());
            int requiredDays = config.get("requiredDays").asInt();
            boolean consecutive = config.has("consecutive") && config.get("consecutive").asBoolean();
            int windowDays = config.has("windowDays") ? config.get("windowDays").asInt() : 7;

            // Проверяем текущее действие во временном окне (UTC)
            LocalTime actionTime = TimeUtils.toUtcTime(timestamp);
            if (!TimeUtils.isInTimeWindow(actionTime, startTime, endTime)) {
                return false;
            }

            // Анализируем историю действий
            Instant startDate = TimeUtils.calculateWindowStart(timestamp, windowDays, ChronoUnit.DAYS);
            List<Action> recentActions = actionRepository.findByUserIdAndActionTypeAndTimestampAfterOrderByTimestampDesc(
                    userId, actionType, startDate);

            return checkDayPattern(recentActions, startTime, endTime, requiredDays, consecutive);

        } catch (Exception e) {
            log.error("Ошибка при проверке TimePattern для пользователя {}: {}", userId, e.getMessage(), e);
            return false;
        }
    }

    private boolean checkDayPattern(List<Action> actions, LocalTime start, LocalTime end,
                                    int requiredDays, boolean consecutive) {
        // Собираем уникальные даты с действиями в нужном временном окне
        Set<LocalDate> matchingDates = actions.stream()
                .filter(action -> {
                    LocalTime actionTime = TimeUtils.toUtcTime(action.getTimestamp());
                    return TimeUtils.isInTimeWindow(actionTime, start, end);
                })
                .map(action -> TimeUtils.toUtcDate(action.getTimestamp()))
                .collect(Collectors.toSet());

        if (consecutive) {
            return checkConsecutiveDays(matchingDates, requiredDays);
        } else {
            return matchingDates.size() >= requiredDays;
        }
    }

    private boolean checkConsecutiveDays(Set<LocalDate> dates, int requiredDays) {
        if (dates.size() < requiredDays) return false;

        List<LocalDate> sortedDates = dates.stream().sorted().toList();
        int maxConsecutive = 1;
        int currentConsecutive = 1;

        // Ищем максимальную последовательность дней
        for (int i = 1; i < sortedDates.size(); i++) {
            if (sortedDates.get(i).minusDays(1).equals(sortedDates.get(i - 1))) {
                currentConsecutive++;
                maxConsecutive = Math.max(maxConsecutive, currentConsecutive);
            } else {
                currentConsecutive = 1;
            }
        }

        return maxConsecutive >= requiredDays;
    }
}