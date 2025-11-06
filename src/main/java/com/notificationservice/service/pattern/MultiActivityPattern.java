package com.notificationservice.service.pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.notificationservice.model.Action;
import com.notificationservice.model.TriggerPattern;
import com.notificationservice.repository.ActionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Паттерн для анализа мульти-активности (разные типы действий)
 */
@Component
@RequiredArgsConstructor
public class MultiActivityPattern extends TriggerPattern {

    private final ActionRepository actionRepository;

    @Override
    public boolean matches(String userId, String actionType, Instant timestamp, TriggerPatternContext context) {
        JsonNode config = context.getConfig();

        if (config.has("requiredActionTypes")) {
            // Вариант 1: определенные типы действий
            return checkRequiredActionTypes(userId, timestamp, config);
        } else if (config.has("actionTypes")) {
            // Вариант 2: минимальное количество из списка типов
            return checkActionTypesCount(userId, timestamp, config);
        }

        return false;
    }

    private boolean checkRequiredActionTypes(String userId, Instant timestamp, JsonNode config) {
        JsonNode requiredTypesNode = config.get("requiredActionTypes");
        Set<String> requiredTypes = Arrays.stream(requiredTypesNode.toString().replaceAll("[\\[\\]\"]", "").split(","))
                .map(String::trim)
                .collect(Collectors.toSet());

        int timeWindowHours = config.get("timeWindowHours").asInt();
        int minUniqueTypes = config.get("minUniqueTypes").asInt();

        Instant startTime = timestamp.minus(timeWindowHours, ChronoUnit.HOURS);
        List<Action> recentActions = actionRepository.findByUserIdAndTimestampAfterOrderByTimestampDesc(
                userId, startTime);

        Set<String> actualTypes = recentActions.stream()
                .map(Action::getActionType)
                .filter(requiredTypes::contains)
                .collect(Collectors.toSet());

        return actualTypes.size() >= minUniqueTypes;
    }

    private boolean checkActionTypesCount(String userId, Instant timestamp, JsonNode config) {
        JsonNode actionTypesNode = config.get("actionTypes");
        Set<String> actionTypes = Arrays.stream(actionTypesNode.toString().replaceAll("[\\[\\]\"]", "").split(","))
                .map(String::trim)
                .collect(Collectors.toSet());

        int requiredCount = config.get("requiredCount").asInt();
        int windowDays = config.get("windowDays").asInt();

        Instant startTime = timestamp.minus(windowDays, ChronoUnit.DAYS);
        List<Action> recentActions = actionRepository.findByUserIdAndTimestampAfterOrderByTimestampDesc(
                userId, startTime);

        long relevantActions = recentActions.stream()
                .filter(action -> actionTypes.contains(action.getActionType()))
                .count();

        return relevantActions >= requiredCount;
    }
}