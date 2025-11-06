package com.notificationservice.service.pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificationservice.repository.ActionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Тест для FrequencyPattern
 */
@ExtendWith(MockitoExtension.class)
class FrequencyPatternTest {

    @Mock
    private ActionRepository actionRepository;

    private FrequencyPattern frequencyPattern;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        frequencyPattern = new FrequencyPattern(actionRepository);
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldMatchWhenActionCountExceedsThreshold() throws Exception {
        // given
        String configJson = "{\"actionCount\": 5, \"timeWindowMinutes\": 60}";
        JsonNode config = objectMapper.readTree(configJson);
        TriggerPatternContext context = new TriggerPatternContext(config);

        when(actionRepository.countByUserAndActionTypeAndTimeWindow(
                eq("user1"), eq("COMMENT"), any(Instant.class), any(Instant.class)))
                .thenReturn(6L); // превышает порог

        // when
        boolean result = frequencyPattern.matches("user1", "COMMENT", Instant.now(), context);

        // then
        assertTrue(result);
        verify(actionRepository).countByUserAndActionTypeAndTimeWindow(
                eq("user1"), eq("COMMENT"), any(Instant.class), any(Instant.class));
    }

    @Test
    void shouldNotMatchWhenActionCountBelowThreshold() throws Exception {
        // given
        String configJson = "{\"actionCount\": 5, \"timeWindowMinutes\": 60}";
        JsonNode config = objectMapper.readTree(configJson);
        TriggerPatternContext context = new TriggerPatternContext(config);

        when(actionRepository.countByUserAndActionTypeAndTimeWindow(
                eq("user1"), eq("COMMENT"), any(Instant.class), any(Instant.class)))
                .thenReturn(3L); // ниже порога

        // when
        boolean result = frequencyPattern.matches("user1", "COMMENT", Instant.now(), context);

        // then
        assertFalse(result);
        verify(actionRepository).countByUserAndActionTypeAndTimeWindow(
                eq("user1"), eq("COMMENT"), any(Instant.class), any(Instant.class));
    }

    @Test
    void shouldNotMatchWhenMissingRequiredFields() throws Exception {
        // given
        String configJson = "{\"actionCount\": 5}"; // отсутствует timeWindowMinutes
        JsonNode config = objectMapper.readTree(configJson);
        TriggerPatternContext context = new TriggerPatternContext(config);

        // when
        boolean result = frequencyPattern.matches("user1", "COMMENT", Instant.now(), context);

        // then
        assertFalse(result);
        verifyNoInteractions(actionRepository);
    }

    @Test
    void shouldHandleRepositoryException() throws Exception {
        // given
        String configJson = "{\"actionCount\": 5, \"timeWindowMinutes\": 60}";
        JsonNode config = objectMapper.readTree(configJson);
        TriggerPatternContext context = new TriggerPatternContext(config);

        when(actionRepository.countByUserAndActionTypeAndTimeWindow(
                eq("user1"), eq("COMMENT"), any(Instant.class), any(Instant.class)))
                .thenThrow(new RuntimeException("Database error"));

        // when
        boolean result = frequencyPattern.matches("user1", "COMMENT", Instant.now(), context);

        // then
        assertFalse(result);
        verify(actionRepository).countByUserAndActionTypeAndTimeWindow(
                eq("user1"), eq("COMMENT"), any(Instant.class), any(Instant.class));
    }
}