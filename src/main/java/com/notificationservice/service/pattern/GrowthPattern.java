package com.notificationservice.service.pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.notificationservice.model.TriggerPattern;
import com.notificationservice.repository.ActionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@Slf4j
public class GrowthPattern extends TriggerPattern {

    private final ActionRepository actionRepository;

    public GrowthPattern(ActionRepository actionRepository) {
        this.actionRepository = actionRepository;
    }

    @Override
    public boolean matches(String userId, String actionType, Instant timestamp, TriggerPatternContext context) {
        try {
            JsonNode config = context.getConfig();

            int growthPercent = config.get("growthPercent").asInt();
            int comparePeriodDays = config.get("comparePeriodDays").asInt();
            int minActionCount = config.has("minActionCount") ? config.get("minActionCount").asInt() : 5;

            // Определяем периоды для сравнения
            Instant currentPeriodEnd = timestamp;
            Instant currentPeriodStart = currentPeriodEnd.minus(comparePeriodDays, ChronoUnit.DAYS);
            Instant previousPeriodEnd = currentPeriodStart;
            Instant previousPeriodStart = previousPeriodEnd.minus(comparePeriodDays, ChronoUnit.DAYS);

            // Получаем статистику за текущий период
            long currentPeriodCount = actionRepository.countByUserAndActionTypeAndTimeWindow(
                    userId, actionType, currentPeriodStart, currentPeriodEnd);

            // Получаем статистику за предыдущий период
            long previousPeriodCount = actionRepository.countByUserAndActionTypeAndTimeWindow(
                    userId, actionType, previousPeriodStart, previousPeriodEnd);

            // Проверяем минимальное количество действий
            if (previousPeriodCount < minActionCount || currentPeriodCount < minActionCount) {
                return false;
            }

            // Рассчитываем рост в процентах
            double growth = calculateGrowthPercentage(previousPeriodCount, currentPeriodCount);

            return growth >= growthPercent;
        } catch (Exception e) {
            log.error("Ошибка при проверке GrowthPattern для пользователя {}: {}", userId, e.getMessage(), e);
            return false;
        }
    }

    private double calculateGrowthPercentage(long previousCount, long currentCount) {
        if (previousCount == 0) {
            return currentCount > 0 ? 100.0 : 0.0;
        }

        return ((double) (currentCount - previousCount) / previousCount) * 100;
    }
}