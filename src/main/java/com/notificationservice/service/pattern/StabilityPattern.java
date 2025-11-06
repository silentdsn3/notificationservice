package com.notificationservice.service.pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.notificationservice.model.Action;
import com.notificationservice.model.TriggerPattern;
import com.notificationservice.repository.ActionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Паттерн для анализа стабильности активности
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class StabilityPattern extends TriggerPattern {

    private final ActionRepository actionRepository;

    @Override
    public boolean matches(String userId, String actionType, Instant timestamp, TriggerPatternContext context) {
        try {
            JsonNode config = context.getConfig();

            if (config.has("minDailyAverage")) {
                return checkStableActivity(userId, actionType, timestamp, config);
            } else if (config.has("weekendToWeekdayRatio")) {
                return checkWeekendRatio(userId, actionType, timestamp, config);
            } else if (config.has("trend")) {
                return checkTrend(userId, actionType, timestamp, config);
            } else if (config.has("minWeeklyGrowth")) {
                return checkWeeklyGrowth(userId, actionType, timestamp, config);
            }
            // Для других типов стабильности возвращаем false
            log.debug("StabilityPattern: no supported configuration found for user {}", userId);
            return false;
        } catch (Exception e) {
            log.error("StabilityPattern error for user {}: {}", userId, e.getMessage());
            return false;
        }
    }

    private boolean checkStableActivity(String userId, String actionType, Instant timestamp, JsonNode config) {
        int analysisPeriodDays = config.get("analysisPeriodDays").asInt();
        double minDailyAverage = config.get("minDailyAverage").asDouble();

        Instant startTime = timestamp.minus(analysisPeriodDays, ChronoUnit.DAYS);
        long totalActions = actionRepository.countByUserAndActionTypeAndTimeWindow(
                userId, actionType, startTime, timestamp);

        double dailyAverage = (double) totalActions / analysisPeriodDays;

        boolean matches = dailyAverage >= minDailyAverage;
        if (matches) {
            log.debug("StableActivity pattern matched for user {}: average {} actions per day (min: {})",
                    userId, dailyAverage, minDailyAverage);
        }
        return matches;
    }

    private boolean checkWeekendRatio(String userId, String actionType, Instant timestamp, JsonNode config) {
        double requiredRatio = config.get("weekendToWeekdayRatio").asDouble();
        int minWeekendActions = config.get("minWeekendActions").asInt();
        int periodWeeks = config.has("periodWeeks") ? config.get("periodWeeks").asInt() : 4;
        int periodDays = periodWeeks * 7;

        Instant startTime = timestamp.minus(periodDays, ChronoUnit.DAYS);

        long weekendCount = actionRepository.countWeekendActions(userId, actionType, startTime, timestamp);
        long weekdayCount = actionRepository.countWeekdayActions(userId, actionType, startTime, timestamp);

        if (weekendCount < minWeekendActions || weekdayCount == 0) {
            log.debug("WeekendRatio: insufficient data - weekend: {}, weekday: {}, minWeekend: {}",
                    weekendCount, weekdayCount, minWeekendActions);
            return false;
        }

        double ratio = (double) weekendCount / weekdayCount;

        boolean matches = ratio >= requiredRatio;
        if (matches) {
            log.debug("WeekendRatio pattern matched for user {}: weekend/weekday ratio = {} (required: {})",
                    userId, ratio, requiredRatio);
        } else {
            log.debug("WeekendRatio not matched for user {}: ratio {} < required {}",
                    userId, ratio, requiredRatio);
        }
        return matches;
    }

    private boolean checkTrend(String userId, String actionType, Instant timestamp, JsonNode config) {
        String trend = config.get("trend").asText();
        int minDays = config.get("minDays").asInt();
        double correlationThreshold = config.get("correlationThreshold").asDouble();

        // Для анализа тренда нужна история минимум за minDays дней
        Instant startTime = timestamp.minus(minDays, ChronoUnit.DAYS);
        List<Action> recentActions = actionRepository.findByUserIdAndActionTypeAndTimestampAfterOrderByTimestampDesc(
                userId, actionType, startTime);

        if (recentActions.isEmpty()) {
            log.debug("Trend analysis: no actions found for user {} in last {} days", userId, minDays);
            return false;
        }

        // Группируем действия по дням
        Map<LocalDate, Long> actionsByDay = recentActions.stream()
                .collect(Collectors.groupingBy(
                        action -> java.time.LocalDate.ofInstant(action.getTimestamp(), java.time.ZoneId.of("UTC")),
                        Collectors.counting()
                ));

        // Для простоты: считаем тренд положительным если количество действий растет
        // В реальной системе здесь была бы более сложная статистика
        boolean matches = analyzeSimpleTrend(actionsByDay, trend, minDays);

        if (matches) {
            log.debug("Trend pattern '{}' matched for user {}: {} actions over {} days",
                    trend, userId, recentActions.size(), minDays);
        } else {
            log.debug("Trend pattern '{}' not matched for user {}: insufficient trend data", trend, userId);
        }
        return matches;
    }

    private boolean checkWeeklyGrowth(String userId, String actionType, Instant timestamp, JsonNode config) {
        double minWeeklyGrowth = config.get("minWeeklyGrowth").asDouble();
        int consecutiveWeeks = config.get("consecutiveWeeks").asInt();
        int minBaseActions = config.get("minBaseActions").asInt();

        // Упрощенная проверка: для демонстрации всегда false
        log.debug("WeeklyGrowth pattern check for user {}: requires {}% growth over {} weeks (min base: {})",
                userId, minWeeklyGrowth, consecutiveWeeks, minBaseActions);
        return false;
    }

    private boolean analyzeSimpleTrend(Map<LocalDate, Long> actionsByDay, String expectedTrend, int minDays) {
        if (actionsByDay.size() < minDays) {
            return false;
        }

        // Сортируем дни по дате
        List<LocalDate> sortedDates = actionsByDay.keySet().stream()
                .sorted()
                .collect(Collectors.toList());

        // Простой анализ: проверяем, что последние дни имеют больше действий чем первые
        int recentDays = Math.min(3, sortedDates.size() / 2);
        long recentActions = 0;
        long earlierActions = 0;

        for (int i = 0; i < recentDays; i++) {
            recentActions += actionsByDay.getOrDefault(sortedDates.get(sortedDates.size() - 1 - i), 0L);
        }

        for (int i = 0; i < recentDays; i++) {
            earlierActions += actionsByDay.getOrDefault(sortedDates.get(i), 0L);
        }

        if (earlierActions == 0) {
            return recentActions > 0 && "INCREASING".equals(expectedTrend);
        }

        double growth = (double) (recentActions - earlierActions) / earlierActions * 100;

        if ("INCREASING".equals(expectedTrend)) {
            return growth > 10; // Минимум 10% рост
        } else if ("DECREASING".equals(expectedTrend)) {
            return growth < -10; // Минимум 10% снижение
        } else if ("STABLE".equals(expectedTrend)) {
            return Math.abs(growth) <= 20; // Не более 20% колебаний
        }

        return false;
    }
}