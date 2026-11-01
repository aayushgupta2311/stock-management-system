package com.inventory.stockmgmt.chatbot.nlp;

import com.inventory.stockmgmt.chatbot.model.TimeRange;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.regex.Pattern;

/**
 * Turns phrases like "today", "this week", "last month", "aaj", "is mahine" into a date window.
 * Pure Java (no Spring) so it is easy to unit test. Input must already be normalised.
 */
public final class TimeRangeParser {

    private static final Pattern TODAY = Pattern.compile("\\b(today|aaj)\\b");
    private static final Pattern YESTERDAY = Pattern.compile("\\b(yesterday|kal)\\b");
    private static final Pattern LAST_WEEK = Pattern.compile("\\b(last week|pichle hafte|pichle hafta)\\b");
    private static final Pattern LAST_MONTH = Pattern.compile("\\b(last month|pichle mahine|pichle mahina)\\b");
    private static final Pattern THIS_WEEK = Pattern.compile("\\b(this week|week|hafte|hafta)\\b");
    private static final Pattern THIS_MONTH = Pattern.compile("\\b(this month|month|mahine|mahina)\\b");
    private static final Pattern THIS_YEAR = Pattern.compile("\\b(this year|year|saal)\\b");

    private TimeRangeParser() {
    }

    public static TimeRange parse(String text, LocalDate today) {
        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime tomorrow = today.plusDays(1).atStartOfDay();
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate firstOfMonth = today.withDayOfMonth(1);

        if (TODAY.matcher(text).find()) {
            return new TimeRange("today", startOfToday, tomorrow);
        }
        if (YESTERDAY.matcher(text).find()) {
            return new TimeRange("yesterday", today.minusDays(1).atStartOfDay(), startOfToday);
        }
        if (LAST_WEEK.matcher(text).find()) {
            return new TimeRange("last week", monday.minusDays(7).atStartOfDay(), monday.atStartOfDay());
        }
        if (LAST_MONTH.matcher(text).find()) {
            return new TimeRange("last month", firstOfMonth.minusMonths(1).atStartOfDay(), firstOfMonth.atStartOfDay());
        }
        if (THIS_WEEK.matcher(text).find()) {
            return new TimeRange("this week", monday.atStartOfDay(), tomorrow);
        }
        if (THIS_MONTH.matcher(text).find()) {
            return new TimeRange("this month", firstOfMonth.atStartOfDay(), tomorrow);
        }
        if (THIS_YEAR.matcher(text).find()) {
            return new TimeRange("this year", today.withDayOfYear(1).atStartOfDay(), tomorrow);
        }
        return new TimeRange("all time", LocalDateTime.of(1970, 1, 1, 0, 0), tomorrow);
    }
}
