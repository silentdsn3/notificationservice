package com.notificationservice.service.pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.notificationservice.model.Action;
import com.notificationservice.model.TriggerPattern;
import com.notificationservice.repository.ActionRepository;
import com.notificationservice.util.TimeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Паттерн для специальных дат (праздники, дни рождения и т.д.)
 */
@Component
@RequiredArgsConstructor
public class SpecialDatePattern extends TriggerPattern {

    private final ActionRepository actionRepository;

    @Override
    public boolean matches(String userId, String actionType, Instant timestamp, TriggerPatternContext context) {
        JsonNode config = context.getConfig();

        LocalDate actionDate = timestamp.atZone(ZoneId.of("UTC")).toLocalDate();

        if (config.has("specialDate")) {
            return checkSpecialDate(userId, timestamp, actionDate, config);
        } else if (config.has("holidays")) {
            return checkHolidayActivity(userId, timestamp, actionDate, config);
        }

        return false;
    }

    private boolean checkSpecialDate(String userId, Instant timestamp, LocalDate actionDate, JsonNode config) {
        String specialDateType = config.get("specialDate").asText();

        // В реальном приложении здесь была бы логика определения особых дат
        if ("USER_BIRTHDAY".equals(specialDateType)) {
            // Заглушка - в реальности проверяли бы базу данных пользователей
            boolean isBirthday = checkUserBirthday(userId, actionDate);
            if (!isBirthday) return false;
        }

        int minActions = config.get("minActions").asInt();
        JsonNode actionTypesNode = config.get("actionTypes");
        Set<String> actionTypes = Arrays.stream(actionTypesNode.toString().replaceAll("[\\[\\]\"]", "").split(","))
                .map(String::trim)
                .collect(Collectors.toSet());

        LocalDateTime startOfDay = actionDate.atStartOfDay();
        LocalDateTime endOfDay = actionDate.plusDays(1).atStartOfDay();

        long actionCount = actionRepository.findByUserIdAndTimestampAfterOrderByTimestampDesc(
                        userId, startOfDay.toInstant(ZoneOffset.UTC)).stream()
                .filter(action -> actionTypes.contains(action.getActionType()))
                .filter(action -> {
                    LocalDate actionLocalDate = TimeUtils.toUtcDate(action.getTimestamp());
                    return actionLocalDate.equals(actionDate);
                })
                .count();

        return actionCount >= minActions;
    }

    private boolean checkHolidayActivity(String userId, Instant timestamp, LocalDate actionDate, JsonNode config) {
        JsonNode holidaysNode = config.get("holidays");
        Set<String> holidays = Arrays.stream(holidaysNode.toString().replaceAll("[\\[\\]\"]", "").split(","))
                .map(String::trim)
                .collect(Collectors.toSet());

        // Заглушка - в реальности проверяли бы календарь праздников
        boolean isHoliday = checkIfHoliday(actionDate, holidays);
        if (!isHoliday) return false;

        // Для праздников просто проверяем наличие активности
        return getDailyActivity(userId, actionDate) > 0;
    }

    private boolean checkUserBirthday(String userId, LocalDate date) {
        // Временная реализация - в продакшене интегрировать с сервисом пользователей
        return date.getDayOfMonth() == 15; // Простая логика для тестирования
    }

    private boolean checkIfHoliday(LocalDate date, Set<String> holidays) {
        // Временная реализация - в продакшене использовать календарь праздников
        return date.getDayOfWeek().getValue() >= 6; // Выходные как "праздники" для теста
    }

    private double getDailyActivity(String userId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        // Подсчитываем общую активность за день (любого типа)
        List<Action> actions = actionRepository.findByUserIdAndTimestampAfterOrderByTimestampDesc(
                userId, startOfDay.toInstant(ZoneOffset.UTC));

        // Фильтруем по дню
        return actions.stream()
                .filter(action -> {
                    LocalDate actionDate = TimeUtils.toUtcDate(action.getTimestamp());
                    return actionDate.equals(date);
                })
                .count();
    }
}