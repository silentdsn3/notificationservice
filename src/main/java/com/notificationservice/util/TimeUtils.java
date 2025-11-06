package com.notificationservice.util;

import lombok.experimental.UtilityClass;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Утилиты для работы со временем в UTC
 */
@UtilityClass
public final class TimeUtils {

    private static final ZoneId UTC_ZONE = ZoneId.of("UTC");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public static LocalDate toUtcDate(Instant instant) {
        return instant.atZone(UTC_ZONE).toLocalDate();
    }

    public static LocalTime toUtcTime(Instant instant) {
        return instant.atZone(UTC_ZONE).toLocalTime();
    }

    public static DayOfWeek toUtcDayOfWeek(Instant instant) {
        return instant.atZone(UTC_ZONE).getDayOfWeek();
    }

    public static LocalTime parseTime(String timeString) {
        return LocalTime.parse(timeString, TIME_FORMATTER);
    }

    /**
     * Проверяет, находится ли время в указанном временном окне
     * Поддерживает окна через полночь (например, 23:00-04:00)
     */
    public static boolean isInTimeWindow(LocalTime time, LocalTime start, LocalTime end) {
        if (start.isBefore(end)) {
            // Обычное окно (например, 10:00-18:00)
            return !time.isBefore(start) && !time.isAfter(end);
        } else {
            // Окно через полночь (например, 23:00-04:00)
            return !time.isBefore(start) || !time.isAfter(end);
        }
    }

    public static Instant calculateWindowStart(Instant timestamp, long amount, ChronoUnit unit) {
        return timestamp.minus(amount, unit);
    }
}