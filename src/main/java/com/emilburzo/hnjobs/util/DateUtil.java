package com.emilburzo.hnjobs.util;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateUtil {

    private static final Pattern PATTERN = Pattern.compile("(\\d+)\\s+(.*?)s? ago");

    public static Instant getAbsoluteDate(String relativeDate) {
        Matcher m = PATTERN.matcher(relativeDate);

        if (m.matches()) {
            int amount = Integer.parseInt(m.group(1));
            String unit = m.group(2);

            var now = ZonedDateTime.now();
            return switch (unit) {
                case "second" -> now.minus(amount, ChronoUnit.SECONDS).toInstant();
                case "minute" -> now.minus(amount, ChronoUnit.MINUTES).toInstant();
                case "hour" -> now.minus(amount, ChronoUnit.HOURS).toInstant();
                case "day" -> now.minusDays(amount).toInstant();
                case "week" -> now.minusWeeks(amount).toInstant();
                case "month" -> now.minusMonths(amount).toInstant();
                case "year" -> now.minusYears(amount).toInstant();
                default -> null;
            };
        }

        return null;
    }
}
