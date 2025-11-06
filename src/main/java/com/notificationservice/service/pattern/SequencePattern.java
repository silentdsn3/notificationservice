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
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Паттерн для анализа последовательностей действий
 * Например: "LOGIN -> READ_ARTICLE -> COMMENT" в течение 30 минут
 */
@Component
@RequiredArgsConstructor
public class SequencePattern extends TriggerPattern {

    private final ActionRepository actionRepository;

    @Override
    public boolean matches(String userId, String actionType, Instant timestamp, TriggerPatternContext context) {
        JsonNode config = context.getConfig();

        // Получаем последовательность действий из конфигурации
        JsonNode sequenceNode = config.get("actionSequence");
        String[] actionSequence = new String[sequenceNode.size()];
        for (int i = 0; i < sequenceNode.size(); i++) {
            actionSequence[i] = sequenceNode.get(i).asText();
        }

        int maxTimeBetweenMinutes = config.get("maxTimeBetweenMinutes").asInt();
        int requiredDays = config.has("requiredDays") ? config.get("requiredDays").asInt() : 1;

        // Проверяем временное окно, если указано
        if (config.has("timeWindow")) {
            String timeWindow = config.get("timeWindow").asText();
            if (!isInTimeWindow(timestamp, timeWindow)) {
                return false;
            }
        }

        // Получаем действия за период
        Instant startTime = timestamp.minus(maxTimeBetweenMinutes * requiredDays, ChronoUnit.MINUTES);
        List<Action> recentActions = actionRepository.findByUserIdAndTimestampAfterOrderByTimestampDesc(
                userId, startTime);

        // Проверяем наличие последовательности
        return containsSequence(recentActions, actionSequence, maxTimeBetweenMinutes, requiredDays);
    }

    private boolean isInTimeWindow(Instant timestamp, String timeWindow) {
        // Формат: "06:00-09:00"
        String[] parts = timeWindow.split("-");
        LocalTime start = TimeUtils.parseTime(parts[0]);
        LocalTime end = TimeUtils.parseTime(parts[1]);
        LocalTime actionTime = TimeUtils.toUtcTime(timestamp);

        return TimeUtils.isInTimeWindow(actionTime, start, end);
    }

    private boolean containsSequence(List<Action> actions, String[] sequence,
                                     int maxTimeBetweenMinutes, int requiredDays) {
        int sequenceFoundDays = 0;

        // Группируем действия по дням
        Map<LocalDate, List<Action>> actionsByDay = actions.stream()
                .collect(Collectors.groupingBy(action -> TimeUtils.toUtcDate(action.getTimestamp())));

        for (List<Action> dailyActions : actionsByDay.values()) {
            if (containsSequenceInDay(dailyActions, sequence, maxTimeBetweenMinutes)) {
                sequenceFoundDays++;
                if (sequenceFoundDays >= requiredDays) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean containsSequenceInDay(List<Action> dailyActions, String[] sequence, int maxTimeBetweenMinutes) {
        // Ищем полную последовательность в течение дня
        for (int i = 0; i <= dailyActions.size() - sequence.length; i++) {
            boolean sequenceMatch = true;

            for (int j = 0; j < sequence.length; j++) {
                if (i + j >= dailyActions.size()) {
                    sequenceMatch = false;
                    break;
                }

                Action action = dailyActions.get(i + j);
                long timeDiff = Math.abs(ChronoUnit.MINUTES.between(
                        dailyActions.get(i).getTimestamp(), action.getTimestamp()));

                if (!action.getActionType().equals(sequence[j]) || timeDiff > maxTimeBetweenMinutes) {
                    sequenceMatch = false;
                    break;
                }
            }

            if (sequenceMatch) {
                return true;
            }
        }

        return false;
    }
}