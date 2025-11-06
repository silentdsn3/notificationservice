package com.notificationservice.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificationservice.model.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Set;

/**
 * Валидатор конфигураций триггеров
 * Улучшает надежность системы путем проверки конфигураций
 */
@Component
public class ConfigurationValidator {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationValidator.class);
    private static final Set<String> VALID_PATTERN_TYPES = Set.of(
            "FREQUENCY", "TIME_PATTERN", "WEEKDAY_PATTERN", "GROWTH",
            "SEQUENCE", "MULTI_ACTIVITY", "SPECIAL_DATE", "STABILITY"
    );
    private static final Set<String> VALID_DAYS = Set.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY");

    private final ObjectMapper objectMapper;

    public ConfigurationValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Валидирует конфигурацию триггера
     */
    public boolean validateTriggerConfig(Trigger trigger) {
        try {
            // Базовая валидация полей
            if (!isValidBaseFields(trigger)) {
                return false;
            }

            // Валидация типа паттерна
            if (!VALID_PATTERN_TYPES.contains(trigger.getPatternType())) {
                logger.error("Невалидный тип паттерна: {}", trigger.getPatternType());
                return false;
            }

            // Валидация JSON конфигурации
            JsonNode config = parseAndValidateJsonConfig(trigger);
            if (config == null) {
                return false;
            }

            // Специфичная валидация для каждого типа паттерна
            return validatePatternSpecificConfig(trigger.getPatternType(), config);

        } catch (Exception e) {
            logger.error("Ошибка при валидации конфигурации триггера {}: {}", trigger.getId(), e.getMessage(), e);
            return false;
        }
    }

    private boolean isValidBaseFields(Trigger trigger) {
        if (trigger.getId() == null || trigger.getId().trim().isEmpty()) {
            logger.error("ID триггера не может быть пустым");
            return false;
        }
        if (trigger.getName() == null || trigger.getName().trim().isEmpty()) {
            logger.error("Название триггера не может быть пустым");
            return false;
        }
        if (trigger.getActionType() == null || trigger.getActionType().trim().isEmpty()) {
            logger.error("Тип действия триггера не может быть пустым");
            return false;
        }
        if (trigger.getMessageTemplate() == null || trigger.getMessageTemplate().trim().isEmpty()) {
            logger.error("Шаблон сообщения триггера не может быть пустым");
            return false;
        }
        if (trigger.getCooldownHours() < 0) {
            logger.error("Cooldown часов не может быть отрицательным");
            return false;
        }
        return true;
    }

    private JsonNode parseAndValidateJsonConfig(Trigger trigger) {
        try {
            if (trigger.getConditionConfig() == null || trigger.getConditionConfig().trim().isEmpty()) {
                logger.error("Конфигурация условий не может быть пустой для триггера {}", trigger.getId());
                return null;
            }

            return objectMapper.readTree(trigger.getConditionConfig());

        } catch (JsonProcessingException e) {
            logger.error("Невалидный JSON в конфигурации триггера {}: {}", trigger.getId(), trigger.getConditionConfig());
            return null;
        }
    }

    private boolean validatePatternSpecificConfig(String patternType, JsonNode config) {
        return switch (patternType) {
            case "FREQUENCY" -> validateFrequencyConfig(config);
            case "TIME_PATTERN" -> validateTimePatternConfig(config);
            case "WEEKDAY_PATTERN" -> validateWeekdayPatternConfig(config);
            case "GROWTH" -> validateGrowthConfig(config);
            case "SEQUENCE" -> validateSequenceConfig(config);
            case "MULTI_ACTIVITY" -> validateMultiActivityConfig(config);
            case "SPECIAL_DATE" -> validateSpecialDateConfig(config);
            case "STABILITY" -> validateStabilityConfig(config);
            default -> false;
        };
    }

    private boolean validateFrequencyConfig(JsonNode config) {
        if (!config.has("actionCount") || !config.get("actionCount").isInt() || config.get("actionCount").asInt() <= 0) {
            logger.error("FrequencyPattern требует положительный actionCount");
            return false;
        }
        if (!config.has("timeWindowMinutes") || !config.get("timeWindowMinutes").isInt() || config.get("timeWindowMinutes").asInt() <= 0) {
            logger.error("FrequencyPattern требует положительный timeWindowMinutes");
            return false;
        }
        return true;
    }

    private boolean validateTimePatternConfig(JsonNode config) {
        try {
            if (!config.has("startTime") || !isValidTime(config.get("startTime").asText())) {
                logger.error("TimePattern требует валидный startTime");
                return false;
            }
            if (!config.has("endTime") || !isValidTime(config.get("endTime").asText())) {
                logger.error("TimePattern требует валидный endTime");
                return false;
            }
            if (!config.has("requiredDays") || !config.get("requiredDays").isInt() || config.get("requiredDays").asInt() <= 0) {
                logger.error("TimePattern требует положительный requiredDays");
                return false;
            }
            return true;
        } catch (DateTimeParseException e) {
            logger.error("Невалидный формат времени в TimePattern: {}", e.getMessage());
            return false;
        }
    }

    private boolean validateWeekdayPatternConfig(JsonNode config) {
        if (!config.has("requiredDays") || !config.get("requiredDays").isArray() || config.get("requiredDays").isEmpty()) {
            logger.error("WeekdayPattern требует непустой массив requiredDays");
            return false;
        }

        for (JsonNode dayNode : config.get("requiredDays")) {
            if (!dayNode.isTextual() || !VALID_DAYS.contains(dayNode.asText().toUpperCase())) {
                logger.error("Невалидный день недели: {}", dayNode.asText());
                return false;
            }
        }

        return true;
    }

    private boolean validateGrowthConfig(JsonNode config) {
        if (!config.has("growthPercent") || !config.get("growthPercent").isInt()) {
            logger.error("GrowthPattern требует growthPercent");
            return false;
        }
        if (!config.has("comparePeriodDays") || !config.get("comparePeriodDays").isInt() || config.get("comparePeriodDays").asInt() <= 0) {
            logger.error("GrowthPattern требует положительный comparePeriodDays");
            return false;
        }
        return true;
    }

    private boolean validateSequenceConfig(JsonNode config) {
        if (!config.has("actionSequence") || !config.get("actionSequence").isArray()) {
            logger.error("SequencePattern требует массив actionSequence");
            return false;
        }
        if (!config.has("maxTimeBetweenMinutes") || config.get("maxTimeBetweenMinutes").asInt() <= 0) {
            logger.error("SequencePattern требует положительный maxTimeBetweenMinutes");
            return false;
        }
        return true;
    }

    private boolean validateMultiActivityConfig(JsonNode config) {
        if (config.has("requiredActionTypes")) {
            if (!config.get("requiredActionTypes").isArray() || config.get("requiredActionTypes").isEmpty()) {
                logger.error("MultiActivityPattern требует непустой массив requiredActionTypes");
                return false;
            }
        } else if (config.has("actionTypes")) {
            if (!config.get("actionTypes").isArray() || config.get("actionTypes").isEmpty()) {
                logger.error("MultiActivityPattern требует непустой массив actionTypes");
                return false;
            }
        } else {
            logger.error("MultiActivityPattern требует requiredActionTypes или actionTypes");
            return false;
        }
        return true;
    }

    private boolean validateSpecialDateConfig(JsonNode config) {
        if (config.has("specialDate")) {
            // Валидация для специальных дат (дни рождения и т.д.)
            String specialDate = config.get("specialDate").asText();
            Set<String> validSpecialDates = Set.of("USER_BIRTHDAY", "REGISTRATION_ANNIVERSARY", "NEW_YEAR_EVE");
            if (!validSpecialDates.contains(specialDate)) {
                logger.error("SpecialDatePattern требует валидный specialDate: {}", validSpecialDates);
                return false;
            }
            if (!config.has("minActions") || !config.get("minActions").isInt() ||
                    config.get("minActions").asInt() <= 0) {
                logger.error("SpecialDatePattern требует положительный minActions");
                return false;
            }
            if (!config.has("actionTypes") || !config.get("actionTypes").isArray() ||
                    config.get("actionTypes").isEmpty()) {
                logger.error("SpecialDatePattern требует непустой массив actionTypes");
                return false;
            }
        } else if (config.has("holidays")) {
            // Валидация для праздничных паттернов
            if (!config.get("holidays").isArray() || config.get("holidays").isEmpty()) {
                logger.error("SpecialDatePattern требует непустой массив holidays");
                return false;
            }

            // Проверяем валидность названий праздников
            Set<String> validHolidays = Set.of("NEW_YEAR", "CHRISTMAS", "BLACK_FRIDAY", "VALENTINES_DAY",
                    "EASTER", "HALLOWEEN", "THANKSGIVING");
            for (JsonNode holidayNode : config.get("holidays")) {
                if (!holidayNode.isTextual() || !validHolidays.contains(holidayNode.asText())) {
                    logger.error("Невалидный праздник: {}", holidayNode.asText());
                    return false;
                }
            }

            if (!config.has("purchaseIncreasePercent") || !config.get("purchaseIncreasePercent").isInt()) {
                logger.error("SpecialDatePattern требует purchaseIncreasePercent");
                return false;
            }

            int increasePercent = config.get("purchaseIncreasePercent").asInt();
            if (increasePercent < 0 || increasePercent > 1000) {
                logger.error("purchaseIncreasePercent должен быть между 0 и 1000");
                return false;
            }
        } else {
            logger.error("SpecialDatePattern требует specialDate или holidays");
            return false;
        }
        return true;
    }

    private boolean validateStabilityConfig(JsonNode config) {
        if (config.has("minDailyAverage")) {
            // Валидация для стабильной активности
            if (!config.has("minDailyAverage") || !config.get("minDailyAverage").isNumber()) {
                logger.error("StabilityPattern требует minDailyAverage");
                return false;
            }
            if (!config.has("maxVariancePercent") || !config.get("maxVariancePercent").isNumber()) {
                logger.error("StabilityPattern требует maxVariancePercent");
                return false;
            }
            if (!config.has("analysisPeriodDays") || !config.get("analysisPeriodDays").isInt() ||
                    config.get("analysisPeriodDays").asInt() <= 0) {
                logger.error("StabilityPattern требует положительный analysisPeriodDays");
                return false;
            }

            double maxVariance = config.get("maxVariancePercent").asDouble();
            if (maxVariance < 0 || maxVariance > 100) {
                logger.error("maxVariancePercent должен быть между 0 и 100");
                return false;
            }

        } else if (config.has("weekendToWeekdayRatio")) {
            // Валидация для соотношения выходные/будни
            if (!config.has("weekendToWeekdayRatio") || !config.get("weekendToWeekdayRatio").isNumber()) {
                logger.error("StabilityPattern требует weekendToWeekdayRatio");
                return false;
            }
            if (!config.has("minWeekendActions") || !config.get("minWeekendActions").isInt() ||
                    config.get("minWeekendActions").asInt() < 0) {
                logger.error("StabilityPattern требует неотрицательный minWeekendActions");
                return false;
            }
            if (!config.has("periodWeeks") || !config.get("periodWeeks").isInt() ||
                    config.get("periodWeeks").asInt() <= 0) {
                logger.error("StabilityPattern требует положительный periodWeeks");
                return false;
            }

            double ratio = config.get("weekendToWeekdayRatio").asDouble();
            if (ratio < 0) {
                logger.error("weekendToWeekdayRatio не может быть отрицательным");
                return false;
            }

        } else if (config.has("trend")) {
            // Валидация для трендов активности
            String trend = config.get("trend").asText();
            Set<String> validTrends = Set.of("INCREASING", "DECREASING", "STABLE");
            if (!validTrends.contains(trend)) {
                logger.error("StabilityPattern требует валидный trend: {}", validTrends);
                return false;
            }
            if (!config.has("minDays") || !config.get("minDays").isInt() ||
                    config.get("minDays").asInt() <= 0) {
                logger.error("StabilityPattern требует положительный minDays");
                return false;
            }
            if (!config.has("correlationThreshold") || !config.get("correlationThreshold").isNumber()) {
                logger.error("StabilityPattern требует correlationThreshold");
                return false;
            }

            double correlation = config.get("correlationThreshold").asDouble();
            if (correlation < 0 || correlation > 1) {
                logger.error("correlationThreshold должен быть между 0 и 1");
                return false;
            }

        } else if (config.has("minWeeklyGrowth")) {
            // Валидация для недельного роста
            if (!config.has("minWeeklyGrowth") || !config.get("minWeeklyGrowth").isNumber()) {
                logger.error("StabilityPattern требует minWeeklyGrowth");
                return false;
            }
            if (!config.has("consecutiveWeeks") || !config.get("consecutiveWeeks").isInt() ||
                    config.get("consecutiveWeeks").asInt() <= 0) {
                logger.error("StabilityPattern требует положительный consecutiveWeeks");
                return false;
            }
            if (!config.has("minBaseActions") || !config.get("minBaseActions").isInt() ||
                    config.get("minBaseActions").asInt() < 0) {
                logger.error("StabilityPattern требует неотрицательный minBaseActions");
                return false;
            }

            double weeklyGrowth = config.get("minWeeklyGrowth").asDouble();
            if (weeklyGrowth < 0 || weeklyGrowth > 1000) {
                logger.error("minWeeklyGrowth должен быть между 0 и 1000");
                return false;
            }

        } else if (config.has("patternType") && "SEASONAL".equals(config.get("patternType").asText())) {
            // Валидация для сезонных паттернов
            if (!config.has("season")) {
                logger.error("Сезонный StabilityPattern требует season");
                return false;
            }
            Set<String> validSeasons = Set.of("WINTER", "SPRING", "SUMMER", "AUTUMN");
            String season = config.get("season").asText();
            if (!validSeasons.contains(season)) {
                logger.error("Невалидный сезон: {}", season);
                return false;
            }
            if (!config.has("activityChange")) {
                logger.error("Сезонный StabilityPattern требует activityChange");
                return false;
            }
            Set<String> validChanges = Set.of("INCREASE", "DECREASE");
            String change = config.get("activityChange").asText();
            if (!validChanges.contains(change)) {
                logger.error("Невалидный activityChange: {}", change);
                return false;
            }
            if (!config.has("minWeeks") || !config.get("minWeeks").isInt() ||
                    config.get("minWeeks").asInt() <= 0) {
                logger.error("Сезонный StabilityPattern требует положительный minWeeks");
                return false;
            }

        } else if (config.has("maxDailyVariance")) {
            // Валидация для недельного баланса
            if (!config.has("maxDailyVariance") || !config.get("maxDailyVariance").isNumber()) {
                logger.error("StabilityPattern требует maxDailyVariance");
                return false;
            }
            if (!config.has("minWeeklyTotal") || !config.get("minWeeklyTotal").isInt() ||
                    config.get("minWeeklyTotal").asInt() <= 0) {
                logger.error("StabilityPattern требует положительный minWeeklyTotal");
                return false;
            }
            if (!config.has("evaluationWeeks") || !config.get("evaluationWeeks").isInt() ||
                    config.get("evaluationWeeks").asInt() <= 0) {
                logger.error("StabilityPattern требует положительный evaluationWeeks");
                return false;
            }

            double maxVariance = config.get("maxDailyVariance").asDouble();
            if (maxVariance < 0 || maxVariance > 100) {
                logger.error("maxDailyVariance должен быть между 0 и 100");
                return false;
            }

        } else {
            logger.error("StabilityPattern требует один из параметров: minDailyAverage, weekendToWeekdayRatio, trend, minWeeklyGrowth, patternType, maxDailyVariance");
            return false;
        }

        return true;
    }

    private boolean isValidTime(String timeString) {
        try {
            LocalTime.parse(timeString);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}