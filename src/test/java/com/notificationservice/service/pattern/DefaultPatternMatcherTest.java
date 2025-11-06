package com.notificationservice.service.pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificationservice.model.Trigger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Тест для DefaultPatternMatcher
 */
@ExtendWith(MockitoExtension.class)
class DefaultPatternMatcherTest {

    @Mock
    private FrequencyPattern frequencyPattern;

    @Mock
    private TimePattern timePattern;

    @Mock
    private WeekdayPattern weekdayPattern;

    @Mock
    private GrowthPattern growthPattern;

    @Mock
    private SequencePattern sequencePattern;

    @Mock
    private MultiActivityPattern multiActivityPattern;

    @Mock
    private SpecialDatePattern specialDatePattern;

    @Mock
    private StabilityPattern stabilityPattern;

    private ObjectMapper objectMapper;
    private DefaultPatternMatcher patternMatcher;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        patternMatcher = new DefaultPatternMatcher(
                frequencyPattern, timePattern, weekdayPattern, growthPattern,
                sequencePattern, multiActivityPattern, specialDatePattern,
                stabilityPattern, objectMapper
        );
    }

    @Test
    void shouldUseFrequencyPatternForFrequencyType() throws Exception {
        // given
        Trigger trigger = createTrigger("FREQUENCY", "{\"actionCount\": 5, \"timeWindowMinutes\": 60}");
        when(frequencyPattern.matches(eq("user1"), eq("COMMENT"), any(Instant.class), any(TriggerPatternContext.class)))
                .thenReturn(true);

        // when
        boolean result = patternMatcher.matches(trigger, "user1", "COMMENT", Instant.now());

        // then
        assertTrue(result);
        verify(frequencyPattern).matches(eq("user1"), eq("COMMENT"), any(Instant.class), any(TriggerPatternContext.class));
    }

    @Test
    void shouldUseTimePatternForTimePatternType() throws Exception {
        // given
        Trigger trigger = createTrigger("TIME_PATTERN", "{\"startTime\": \"23:00\", \"endTime\": \"04:00\", \"requiredDays\": 3}");
        when(timePattern.matches(anyString(), anyString(), any(Instant.class), any(TriggerPatternContext.class)))
                .thenReturn(true);

        // when
        boolean result = patternMatcher.matches(trigger, "user1", "COMMENT", Instant.now());

        // then
        assertTrue(result);
        verify(timePattern).matches(eq("user1"), eq("COMMENT"), any(Instant.class), any(TriggerPatternContext.class));
    }

    @Test
    void shouldUseWeekdayPatternForWeekdayPatternType() throws Exception {
        // given
        Trigger trigger = createTrigger("WEEKDAY_PATTERN", "{\"requiredDays\": [\"SATURDAY\", \"SUNDAY\"], \"windowWeeks\": 3}");
        when(weekdayPattern.matches(anyString(), anyString(), any(Instant.class), any(TriggerPatternContext.class)))
                .thenReturn(true);

        // when
        boolean result = patternMatcher.matches(trigger, "user1", "COMMENT", Instant.now());

        // then
        assertTrue(result);
        verify(weekdayPattern).matches(eq("user1"), eq("COMMENT"), any(Instant.class), any(TriggerPatternContext.class));
    }

    @Test
    void shouldUseGrowthPatternForGrowthType() throws Exception {
        // given
        Trigger trigger = createTrigger("GROWTH", "{\"growthPercent\": 50, \"comparePeriodDays\": 7, \"minActionCount\": 10}");
        when(growthPattern.matches(anyString(), anyString(), any(Instant.class), any(TriggerPatternContext.class)))
                .thenReturn(true);

        // when
        boolean result = patternMatcher.matches(trigger, "user1", "COMMENT", Instant.now());

        // then
        assertTrue(result);
        verify(growthPattern).matches(eq("user1"), eq("COMMENT"), any(Instant.class), any(TriggerPatternContext.class));
    }

    @Test
    void shouldUseSequencePatternForSequenceType() throws Exception {
        // given
        Trigger trigger = createTrigger("SEQUENCE", "{\"actionSequence\": [\"LOGIN\", \"READ_ARTICLE\", \"COMMENT\"], \"maxTimeBetweenMinutes\": 30}");
        when(sequencePattern.matches(anyString(), anyString(), any(Instant.class), any(TriggerPatternContext.class)))
                .thenReturn(true);

        // when
        boolean result = patternMatcher.matches(trigger, "user1", "COMMENT", Instant.now());

        // then
        assertTrue(result);
        verify(sequencePattern).matches(eq("user1"), eq("COMMENT"), any(Instant.class), any(TriggerPatternContext.class));
    }

    @Test
    void shouldUseMultiActivityPatternForMultiActivityType() throws Exception {
        // given
        Trigger trigger = createTrigger("MULTI_ACTIVITY", "{\"requiredActionTypes\": [\"COMMENT\", \"READ_ARTICLE\", \"LIKE\"], \"timeWindowHours\": 24, \"minUniqueTypes\": 3}");
        when(multiActivityPattern.matches(anyString(), anyString(), any(Instant.class), any(TriggerPatternContext.class)))
                .thenReturn(true);

        // when
        boolean result = patternMatcher.matches(trigger, "user1", "COMMENT", Instant.now());

        // then
        assertTrue(result);
        verify(multiActivityPattern).matches(eq("user1"), eq("COMMENT"), any(Instant.class), any(TriggerPatternContext.class));
    }

    @Test
    void shouldUseSpecialDatePatternForSpecialDateType() throws Exception {
        // given
        Trigger trigger = createTrigger("SPECIAL_DATE", "{\"specialDate\": \"USER_BIRTHDAY\", \"minActions\": 5, \"actionTypes\": [\"LOGIN\", \"COMMENT\", \"PURCHASE\"]}");
        when(specialDatePattern.matches(anyString(), anyString(), any(Instant.class), any(TriggerPatternContext.class)))
                .thenReturn(true);

        // when
        boolean result = patternMatcher.matches(trigger, "user1", "COMMENT", Instant.now());

        // then
        assertTrue(result);
        verify(specialDatePattern).matches(eq("user1"), eq("COMMENT"), any(Instant.class), any(TriggerPatternContext.class));
    }

    @Test
    void shouldUseStabilityPatternForStabilityType() throws Exception {
        // given
        Trigger trigger = createTrigger("STABILITY", "{\"minDailyAverage\": 5, \"maxVariancePercent\": 30, \"analysisPeriodDays\": 14}");
        when(stabilityPattern.matches(anyString(), anyString(), any(Instant.class), any(TriggerPatternContext.class)))
                .thenReturn(true);

        // when
        boolean result = patternMatcher.matches(trigger, "user1", "COMMENT", Instant.now());

        // then
        assertTrue(result);
        verify(stabilityPattern).matches(eq("user1"), eq("COMMENT"), any(Instant.class), any(TriggerPatternContext.class));
    }

    @Test
    void shouldReturnFalseForUnknownPatternType() throws Exception {
        // given
        Trigger trigger = createTrigger("UNKNOWN_TYPE", "{}");

        // when
        boolean result = patternMatcher.matches(trigger, "user1", "COMMENT", Instant.now());

        // then
        assertFalse(result);
        verifyNoInteractions(frequencyPattern, timePattern, weekdayPattern, growthPattern,
                sequencePattern, multiActivityPattern, specialDatePattern, stabilityPattern);
    }

    @Test
    void shouldReturnFalseForInvalidJsonConfig() {
        // given
        Trigger trigger = createTrigger("FREQUENCY", "invalid json");

        // when
        boolean result = patternMatcher.matches(trigger, "user1", "COMMENT", Instant.now());

        // then
        assertFalse(result);
        verifyNoInteractions(frequencyPattern, timePattern, weekdayPattern, growthPattern,
                sequencePattern, multiActivityPattern, specialDatePattern, stabilityPattern);
    }

    @Test
    void shouldHandleExceptionInPatternMatching() throws Exception {
        // given
        Trigger trigger = createTrigger("FREQUENCY", "{\"actionCount\": 5, \"timeWindowMinutes\": 60}");
        when(frequencyPattern.matches(anyString(), anyString(), any(Instant.class), any(TriggerPatternContext.class)))
                .thenThrow(new RuntimeException("Pattern error"));

        // when
        boolean result = patternMatcher.matches(trigger, "user1", "COMMENT", Instant.now());

        // then
        assertFalse(result);
        verify(frequencyPattern).matches(eq("user1"), eq("COMMENT"), any(Instant.class), any(TriggerPatternContext.class));
    }

    @Test
    void shouldReturnFalseWhenPatternReturnsFalse() throws Exception {
        // given
        Trigger trigger = createTrigger("FREQUENCY", "{\"actionCount\": 5, \"timeWindowMinutes\": 60}");
        when(frequencyPattern.matches(eq("user1"), eq("COMMENT"), any(Instant.class), any(TriggerPatternContext.class)))
                .thenReturn(false);

        // when
        boolean result = patternMatcher.matches(trigger, "user1", "COMMENT", Instant.now());

        // then
        assertFalse(result);
        verify(frequencyPattern).matches(eq("user1"), eq("COMMENT"), any(Instant.class), any(TriggerPatternContext.class));
    }

    private Trigger createTrigger(String patternType, String conditionConfig) {
        Trigger trigger = new Trigger();
        trigger.setId("TEST_" + patternType);
        trigger.setName("Test " + patternType);
        trigger.setActionType("COMMENT");
        trigger.setPatternType(patternType);
        trigger.setConditionConfig(conditionConfig);
        trigger.setMessageTemplate("Test message");
        return trigger;
    }
}