package com.notificationservice.service.pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificationservice.model.Trigger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Основная реализация PatternMatcher
 * Делегирует проверку конкретным паттернам в зависимости от типа
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DefaultPatternMatcher implements PatternMatcher {

    private final FrequencyPattern frequencyPattern;
    private final TimePattern timePattern;
    private final WeekdayPattern weekdayPattern;
    private final GrowthPattern growthPattern;
    private final SequencePattern sequencePattern;
    private final MultiActivityPattern multiActivityPattern;
    private final SpecialDatePattern specialDatePattern;
    private final StabilityPattern stabilityPattern;
    private final ObjectMapper objectMapper;

    @Override
    public boolean matches(Trigger trigger, String userId, String actionType, Instant timestamp) {
        try {
            TriggerPatternContext context = new TriggerPatternContext(
                objectMapper.readTree(trigger.getConditionConfig()));

            boolean result = switch (trigger.getPatternType()) {
                case "FREQUENCY" -> frequencyPattern.matches(userId, actionType, timestamp, context);
                case "TIME_PATTERN" -> timePattern.matches(userId, actionType, timestamp, context);
                case "WEEKDAY_PATTERN" -> weekdayPattern.matches(userId, actionType, timestamp, context);
                case "GROWTH" -> growthPattern.matches(userId, actionType, timestamp, context);
                case "SEQUENCE" -> sequencePattern.matches(userId, actionType, timestamp, context);
                case "MULTI_ACTIVITY" -> multiActivityPattern.matches(userId, actionType, timestamp, context);
                case "SPECIAL_DATE" -> specialDatePattern.matches(userId, actionType, timestamp, context);
                case "STABILITY" -> stabilityPattern.matches(userId, actionType, timestamp, context);
                default -> {
                    log.warn("Неизвестный тип паттерна: {}", trigger.getPatternType());
                    yield false;
                }
            };

            if (result) {
                log.info("Pattern MATCHED: trigger={} ({}), user={}, actionType={}",
                         trigger.getId(), trigger.getPatternType(), userId, actionType);
            } else {
                log.debug("Pattern NOT matched: trigger={} ({}), user={}, actionType={}",
                         trigger.getId(), trigger.getPatternType(), userId, actionType);
            }

            return result;

        } catch (Exception e) {
            log.error("Ошибка при сопоставлении паттерна для триггера {}: {}",
                     trigger.getId(), e.getMessage());
            return false;
        }
    }
}