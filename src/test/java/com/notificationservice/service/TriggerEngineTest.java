package com.notificationservice.service;

import com.notificationservice.config.TriggerProvider;
import com.notificationservice.model.Trigger;
import com.notificationservice.service.pattern.PatternMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Пример теста, демонстрирующего улучшенную тестируемость
 * Благодаря интерфейсу PatternMatcher легко мокировать зависимости
 */
@ExtendWith(MockitoExtension.class)
class TriggerEngineTest {

    @Mock
    private TriggerProvider triggerProvider;

    @Mock
    private PatternMatcher patternMatcher;

    private TriggerEngine triggerEngine;

    @BeforeEach
    void setUp() {
        triggerEngine = new TriggerEngine(triggerProvider, patternMatcher);
    }

    @Test
    void shouldReturnEmptyListWhenNoTriggersForActionType() {
        // given
        when(triggerProvider.getTriggersByActionType("COMMENT")).thenReturn(List.of());

        // when
        List<Trigger> result = triggerEngine.checkTriggers("user1", "COMMENT", Instant.now());

        // then
        assertTrue(result.isEmpty());
        verify(triggerProvider).getTriggersByActionType("COMMENT");
        verifyNoInteractions(patternMatcher);
    }

    @Test
    void shouldReturnOnlyMatchingTriggers() {
        // given
        Trigger trigger1 = createTrigger("TRIGGER_1", "COMMENT");
        Trigger trigger2 = createTrigger("TRIGGER_2", "COMMENT");

        when(triggerProvider.getTriggersByActionType("COMMENT")).thenReturn(List.of(trigger1, trigger2));
        when(patternMatcher.matches(eq(trigger1), eq("user1"), eq("COMMENT"), any(Instant.class))).thenReturn(true);
        when(patternMatcher.matches(eq(trigger2), eq("user1"), eq("COMMENT"), any(Instant.class))).thenReturn(false);

        // when
        List<Trigger> result = triggerEngine.checkTriggers("user1", "COMMENT", Instant.now());

        // then
        assertEquals(1, result.size());
        assertEquals("TRIGGER_1", result.get(0).getId());
        verify(patternMatcher).matches(eq(trigger1), eq("user1"), eq("COMMENT"), any(Instant.class));
        verify(patternMatcher).matches(eq(trigger2), eq("user1"), eq("COMMENT"), any(Instant.class));
    }

    @Test
    void shouldHandleExceptionInPatternMatcher() {
        // given
        Trigger trigger = createTrigger("TRIGGER_1", "COMMENT");
        when(triggerProvider.getTriggersByActionType("COMMENT")).thenReturn(List.of(trigger));
        when(patternMatcher.matches(eq(trigger), eq("user1"), eq("COMMENT"), any(Instant.class)))
                .thenThrow(new RuntimeException("Test exception"));

        // when
        List<Trigger> result = triggerEngine.checkTriggers("user1", "COMMENT", Instant.now());

        // then
        assertTrue(result.isEmpty());
        verify(patternMatcher).matches(eq(trigger), eq("user1"), eq("COMMENT"), any(Instant.class));
    }

    @Test
    void shouldReturnMultipleMatchingTriggers() {
        // given
        Trigger trigger1 = createTrigger("TRIGGER_1", "PURCHASE");
        Trigger trigger2 = createTrigger("TRIGGER_2", "PURCHASE");
        Trigger trigger3 = createTrigger("TRIGGER_3", "PURCHASE");

        when(triggerProvider.getTriggersByActionType("PURCHASE")).thenReturn(List.of(trigger1, trigger2, trigger3));
        when(patternMatcher.matches(eq(trigger1), eq("user2"), eq("PURCHASE"), any(Instant.class))).thenReturn(true);
        when(patternMatcher.matches(eq(trigger2), eq("user2"), eq("PURCHASE"), any(Instant.class))).thenReturn(true);
        when(patternMatcher.matches(eq(trigger3), eq("user2"), eq("PURCHASE"), any(Instant.class))).thenReturn(false);

        // when
        List<Trigger> result = triggerEngine.checkTriggers("user2", "PURCHASE", Instant.now());

        // then
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(t -> t.getId().equals("TRIGGER_1")));
        assertTrue(result.stream().anyMatch(t -> t.getId().equals("TRIGGER_2")));
        assertFalse(result.stream().anyMatch(t -> t.getId().equals("TRIGGER_3")));
    }

    @Test
    void shouldContinueProcessingWhenOneTriggerFails() {
        // given
        Trigger trigger1 = createTrigger("TRIGGER_1", "COMMENT");
        Trigger trigger2 = createTrigger("TRIGGER_2", "COMMENT");
        Trigger trigger3 = createTrigger("TRIGGER_3", "COMMENT");

        when(triggerProvider.getTriggersByActionType("COMMENT")).thenReturn(List.of(trigger1, trigger2, trigger3));
        when(patternMatcher.matches(eq(trigger1), eq("user1"), eq("COMMENT"), any(Instant.class))).thenReturn(true);
        when(patternMatcher.matches(eq(trigger2), eq("user1"), eq("COMMENT"), any(Instant.class)))
                .thenThrow(new RuntimeException("Test exception"));
        when(patternMatcher.matches(eq(trigger3), eq("user1"), eq("COMMENT"), any(Instant.class))).thenReturn(true);

        // when
        List<Trigger> result = triggerEngine.checkTriggers("user1", "COMMENT", Instant.now());

        // then
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(t -> t.getId().equals("TRIGGER_1")));
        assertTrue(result.stream().anyMatch(t -> t.getId().equals("TRIGGER_3")));
        assertFalse(result.stream().anyMatch(t -> t.getId().equals("TRIGGER_2")));
    }

    private Trigger createTrigger(String id, String actionType) {
        Trigger trigger = new Trigger();
        trigger.setId(id);
        trigger.setName("Test Trigger " + id);
        trigger.setActionType(actionType);
        trigger.setPatternType("FREQUENCY");
        trigger.setConditionConfig("{\"actionCount\": 5, \"timeWindowMinutes\": 60}");
        trigger.setMessageTemplate("Test message for " + id);
        trigger.setCooldownHours(24);
        return trigger;
    }
}