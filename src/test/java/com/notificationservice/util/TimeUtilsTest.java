package com.notificationservice.util;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тест для утилит работы со временем
 */
class TimeUtilsTest {

    @Test
    void shouldParseTimeCorrectly() {
        // when
        LocalTime time = TimeUtils.parseTime("14:30");

        // then
        assertEquals(14, time.getHour());
        assertEquals(30, time.getMinute());
    }

    @Test
    void shouldCheckTimeInNormalWindow() {
        // given
        LocalTime time = LocalTime.of(12, 0);
        LocalTime start = LocalTime.of(10, 0);
        LocalTime end = LocalTime.of(18, 0);

        // when
        boolean isInWindow = TimeUtils.isInTimeWindow(time, start, end);

        // then
        assertTrue(isInWindow);
    }

    @Test
    void shouldCheckTimeOutsideNormalWindow() {
        // given
        LocalTime time = LocalTime.of(20, 0);
        LocalTime start = LocalTime.of(10, 0);
        LocalTime end = LocalTime.of(18, 0);

        // when
        boolean isInWindow = TimeUtils.isInTimeWindow(time, start, end);

        // then
        assertFalse(isInWindow);
    }

    @Test
    void shouldCheckTimeInOvernightWindow() {
        // given
        LocalTime time = LocalTime.of(2, 0);
        LocalTime start = LocalTime.of(23, 0);
        LocalTime end = LocalTime.of(4, 0);

        // when
        boolean isInWindow = TimeUtils.isInTimeWindow(time, start, end);

        // then
        assertTrue(isInWindow);
    }

    @Test
    void shouldCheckTimeOutsideOvernightWindow() {
        // given
        LocalTime time = LocalTime.of(10, 0);
        LocalTime start = LocalTime.of(23, 0);
        LocalTime end = LocalTime.of(4, 0);

        // when
        boolean isInWindow = TimeUtils.isInTimeWindow(time, start, end);

        // then
        assertFalse(isInWindow);
    }

    @Test
    void shouldConvertInstantToUtcDate() {
        // given
        Instant instant = Instant.parse("2024-01-15T10:30:00Z");

        // when
        var date = TimeUtils.toUtcDate(instant);

        // then
        assertEquals(2024, date.getYear());
        assertEquals(1, date.getMonthValue());
        assertEquals(15, date.getDayOfMonth());
    }

    @Test
    void shouldCalculateWindowStartCorrectly() {
        // given
        Instant timestamp = Instant.parse("2024-01-15T10:30:00Z");

        // when
        Instant windowStart = TimeUtils.calculateWindowStart(timestamp, 7, java.time.temporal.ChronoUnit.DAYS);

        // then
        assertEquals(Instant.parse("2024-01-08T10:30:00Z"), windowStart);
    }
}
