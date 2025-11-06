package com.notificationservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificationservice.model.Trigger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тест для валидатора конфигураций
 */
class ConfigurationValidatorTest {

    private ConfigurationValidator validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        validator = new ConfigurationValidator(objectMapper);
    }

    @Test
    void shouldValidateCorrectFrequencyTrigger() {
        // given
        Trigger trigger = createValidTrigger();
        trigger.setPatternType("FREQUENCY");
        trigger.setConditionConfig("{\"actionCount\": 5, \"timeWindowMinutes\": 60}");

        // when
        boolean isValid = validator.validateTriggerConfig(trigger);

        // then
        assertTrue(isValid);
    }

    @Test
    void shouldValidateCorrectTimePatternTrigger() {
        // given
        Trigger trigger = createValidTrigger();
        trigger.setPatternType("TIME_PATTERN");
        trigger.setConditionConfig("{\"startTime\": \"23:00\", \"endTime\": \"04:00\", \"requiredDays\": 3}");

        // when
        boolean isValid = validator.validateTriggerConfig(trigger);

        // then
        assertTrue(isValid);
    }

    @Test
    void shouldValidateCorrectWeekdayPatternTrigger() {
        // given
        Trigger trigger = createValidTrigger();
        trigger.setPatternType("WEEKDAY_PATTERN");
        trigger.setConditionConfig("{\"requiredDays\": [\"SATURDAY\", \"SUNDAY\"], \"windowWeeks\": 3}");

        // when
        boolean isValid = validator.validateTriggerConfig(trigger);

        // then
        assertTrue(isValid);
    }

    @Test
    void shouldRejectTriggerWithInvalidJson() {
        // given
        Trigger trigger = createValidTrigger();
        trigger.setConditionConfig("invalid json");

        // when
        boolean isValid = validator.validateTriggerConfig(trigger);

        // then
        assertFalse(isValid);
    }

    @Test
    void shouldRejectTriggerWithMissingRequiredFields() {
        // given
        Trigger trigger = new Trigger();
        trigger.setId(""); // пустой ID
        trigger.setName("Test");
        trigger.setActionType("COMMENT");
        trigger.setPatternType("FREQUENCY");
        trigger.setConditionConfig("{\"actionCount\": 5}");
        trigger.setMessageTemplate("Test message");
        trigger.setCooldownHours(24);

        // when
        boolean isValid = validator.validateTriggerConfig(trigger);

        // then
        assertFalse(isValid);
    }

    @Test
    void shouldRejectTriggerWithInvalidPatternType() {
        // given
        Trigger trigger = createValidTrigger();
        trigger.setPatternType("INVALID_TYPE"); // невалидный тип

        // when
        boolean isValid = validator.validateTriggerConfig(trigger);

        // then
        assertFalse(isValid);
    }

    @Test
    void shouldRejectTriggerWithNegativeCooldown() {
        // given
        Trigger trigger = createValidTrigger();
        trigger.setCooldownHours(-1); // отрицательный cooldown

        // when
        boolean isValid = validator.validateTriggerConfig(trigger);

        // then
        assertFalse(isValid);
    }

    @Test
    void shouldRejectFrequencyTriggerWithoutRequiredFields() {
        // given
        Trigger trigger = createValidTrigger();
        trigger.setPatternType("FREQUENCY");
        trigger.setConditionConfig("{\"actionCount\": 5}"); // отсутствует timeWindowMinutes

        // when
        boolean isValid = validator.validateTriggerConfig(trigger);

        // then
        assertFalse(isValid);
    }

    @Test
    void shouldRejectTimePatternTriggerWithInvalidTimeFormat() {
        // given
        Trigger trigger = createValidTrigger();
        trigger.setPatternType("TIME_PATTERN");
        trigger.setConditionConfig("{\"startTime\": \"25:00\", \"endTime\": \"04:00\", \"requiredDays\": 3}"); // невалидное время

        // when
        boolean isValid = validator.validateTriggerConfig(trigger);

        // then
        assertFalse(isValid);
    }

    @Test
    void shouldRejectWeekdayPatternTriggerWithInvalidDays() {
        // given
        Trigger trigger = createValidTrigger();
        trigger.setPatternType("WEEKDAY_PATTERN");
        trigger.setConditionConfig("{\"requiredDays\": [\"INVALID_DAY\"], \"windowWeeks\": 3}"); // невалидный день

        // when
        boolean isValid = validator.validateTriggerConfig(trigger);

        // then
        assertFalse(isValid);
    }

    @Test
    void shouldRejectSequenceTriggerWithoutActionSequence() {
        // given
        Trigger trigger = createValidTrigger();
        trigger.setPatternType("SEQUENCE");
        trigger.setConditionConfig("""
        {
            "maxTimeBetweenMinutes": 30
        }
        """);

        // when
        boolean isValid = validator.validateTriggerConfig(trigger);

        // then
        assertFalse(isValid);
    }

    @Test
    void shouldRejectMultiActivityTriggerWithoutRequiredFields() {
        // given
        Trigger trigger = createValidTrigger();
        trigger.setPatternType("MULTI_ACTIVITY");
        trigger.setConditionConfig("""
        {
            "requiredActionTypes": ["COMMENT", "READ_ARTICLE"]
        }
        """);

        // when
        boolean isValid = validator.validateTriggerConfig(trigger);

        // then
        assertFalse(isValid);
    }

    @Test
    void shouldRejectSpecialDateTriggerWithInvalidHoliday() {
        // given
        Trigger trigger = createValidTrigger();
        trigger.setPatternType("SPECIAL_DATE");
        trigger.setConditionConfig("""
        {
            "holidays": ["INVALID_HOLIDAY"],
            "purchaseIncreasePercent": 200
        }
        """);

        // when
        boolean isValid = validator.validateTriggerConfig(trigger);

        // then
        assertFalse(isValid);
    }

    @Test
    void shouldRejectStabilityTriggerWithInvalidVariance() {
        // given
        Trigger trigger = createValidTrigger();
        trigger.setPatternType("STABILITY");
        trigger.setConditionConfig("""
        {
            "minDailyAverage": 5,
            "maxVariancePercent": 150,
            "analysisPeriodDays": 14
        }
        """);

        // when
        boolean isValid = validator.validateTriggerConfig(trigger);

        // then
        assertFalse(isValid);
    }

    private Trigger createValidTrigger() {
        Trigger trigger = new Trigger();
        trigger.setId("TEST_TRIGGER");
        trigger.setName("Test Trigger");
        trigger.setActionType("COMMENT");
        trigger.setPatternType("FREQUENCY");
        trigger.setConditionConfig("{\"actionCount\": 5, \"timeWindowMinutes\": 60}");
        trigger.setMessageTemplate("Test message");
        trigger.setCooldownHours(24);
        return trigger;
    }
}
