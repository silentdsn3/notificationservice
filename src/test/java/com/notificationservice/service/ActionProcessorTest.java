package com.notificationservice.service;

import com.notificationservice.config.ConfigurationValidator;
import com.notificationservice.dto.ActionDto;
import com.notificationservice.model.Action;
import com.notificationservice.model.Trigger;
import com.notificationservice.repository.ActionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Тест для ActionProcessor
 */
@ExtendWith(MockitoExtension.class)
class ActionProcessorTest {

    @Mock
    private ActionRepository actionRepository;

    @Mock
    private TriggerEngine triggerEngine;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ConfigurationValidator configurationValidator;

    private ActionProcessor actionProcessor;

    @BeforeEach
    void setUp() {
        actionProcessor = new ActionProcessor(actionRepository, triggerEngine, notificationService, configurationValidator);
    }

    @Test
    void shouldProcessActionAndCreateNotifications() {
        // given
        ActionDto actionDto = new ActionDto("user1", "COMMENT", Instant.now());
        Trigger trigger = createTrigger("TEST_TRIGGER", "COMMENT");

        when(actionRepository.save(any(Action.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(triggerEngine.checkTriggers(eq("user1"), eq("COMMENT"), any(Instant.class)))
                .thenReturn(List.of(trigger));
        when(configurationValidator.validateTriggerConfig(trigger)).thenReturn(true);
        when(notificationService.shouldSendNotification(eq("user1"), eq(trigger))).thenReturn(true);

        // when
        actionProcessor.processAction(actionDto);

        // then
        verify(actionRepository).save(any(Action.class));
        verify(triggerEngine).checkTriggers(eq("user1"), eq("COMMENT"), any(Instant.class));
        verify(notificationService).shouldSendNotification(eq("user1"), eq(trigger));
        verify(notificationService).createNotification(eq("user1"), eq(trigger));
    }

    @Test
    void shouldProcessActionWithoutNotificationsWhenNoTriggers() {
        // given
        ActionDto actionDto = new ActionDto("user1", "COMMENT", Instant.now());

        when(actionRepository.save(any(Action.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(triggerEngine.checkTriggers(eq("user1"), eq("COMMENT"), any(Instant.class)))
                .thenReturn(List.of());

        // when
        actionProcessor.processAction(actionDto);

        // then
        verify(actionRepository).save(any(Action.class));
        verify(triggerEngine).checkTriggers(eq("user1"), eq("COMMENT"), any(Instant.class));
        verify(notificationService, never()).shouldSendNotification(anyString(), any(Trigger.class));
        verify(notificationService, never()).createNotification(anyString(), any(Trigger.class));
    }

    @Test
    void shouldSkipNotificationWhenTriggerConfigInvalid() {
        // given
        ActionDto actionDto = new ActionDto("user1", "COMMENT", Instant.now());
        Trigger trigger = createTrigger("TEST_TRIGGER", "COMMENT");

        when(actionRepository.save(any(Action.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(triggerEngine.checkTriggers(eq("user1"), eq("COMMENT"), any(Instant.class)))
                .thenReturn(List.of(trigger));
        when(configurationValidator.validateTriggerConfig(trigger)).thenReturn(false);

        // when
        actionProcessor.processAction(actionDto);

        // then
        verify(actionRepository).save(any(Action.class));
        verify(triggerEngine).checkTriggers(eq("user1"), eq("COMMENT"), any(Instant.class));
        verify(configurationValidator).validateTriggerConfig(trigger);
        verify(notificationService, never()).shouldSendNotification(anyString(), any(Trigger.class));
        verify(notificationService, never()).createNotification(anyString(), any(Trigger.class));
    }

    @Test
    void shouldSkipNotificationWhenCooldownActive() {
        // given
        ActionDto actionDto = new ActionDto("user1", "COMMENT", Instant.now());
        Trigger trigger = createTrigger("TEST_TRIGGER", "COMMENT");

        when(actionRepository.save(any(Action.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(triggerEngine.checkTriggers(eq("user1"), eq("COMMENT"), any(Instant.class)))
                .thenReturn(List.of(trigger));
        when(configurationValidator.validateTriggerConfig(trigger)).thenReturn(true);
        when(notificationService.shouldSendNotification(eq("user1"), eq(trigger))).thenReturn(false);

        // when
        actionProcessor.processAction(actionDto);

        // then
        verify(actionRepository).save(any(Action.class));
        verify(triggerEngine).checkTriggers(eq("user1"), eq("COMMENT"), any(Instant.class));
        verify(notificationService).shouldSendNotification(eq("user1"), eq(trigger));
        verify(notificationService, never()).createNotification(anyString(), any(Trigger.class));
    }

    @Test
    void shouldThrowExceptionWhenActionProcessingFails() {
        // given
        ActionDto actionDto = new ActionDto("user1", "COMMENT", Instant.now());

        when(actionRepository.save(any(Action.class))).thenThrow(new RuntimeException("Database error"));

        // when & then
        assertThrows(RuntimeException.class, () -> actionProcessor.processAction(actionDto));

        verify(actionRepository).save(any(Action.class));
        verifyNoInteractions(triggerEngine, notificationService, configurationValidator);
    }

    private Trigger createTrigger(String id, String actionType) {
        Trigger trigger = new Trigger();
        trigger.setId(id);
        trigger.setName("Test Trigger");
        trigger.setActionType(actionType);
        trigger.setPatternType("FREQUENCY");
        trigger.setConditionConfig("{\"actionCount\": 5, \"timeWindowMinutes\": 60}");
        trigger.setMessageTemplate("Test message");
        trigger.setCooldownHours(24);
        return trigger;
    }
}
