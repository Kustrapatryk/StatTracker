package com.stattracker.common;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** NBA season identified by the year it starts in; written as {@code 2025-26}. */
public record Season(int startYear) implements Comparable<Season> {

    /** NBA.com publishes game dates in US Eastern time. */
    public static final ZoneId NBA_ZONE = ZoneId.of("America/New_York");

    private static final Pattern ID_PATTERN = Pattern.compile("(\\d{4})-(\\d{2})");

    public Season {
        if (startYear < 1946 || startYear > 2999) {
            throw new IllegalArgumentException("Season start year out of range: " + startYear);
        }
    }

    public static Season parse(String id) {
        Matcher matcher = ID_PATTERN.matcher(id);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid season '" + id + "', expected a value like 2025-26");
        }
        Season season = new Season(Integer.parseInt(matcher.group(1)));
        if (!season.id().equals(id)) {
            throw new IllegalArgumentException("Invalid season '" + id + "', did you mean " + season.id() + "?");
        }
        return season;
    }

    /** Seasons tip off in October; before that the previous season is still the latest one. */
    public static Season current(Clock clock) {
        LocalDate today = LocalDate.now(clock.withZone(NBA_ZONE));
        int year = today.getYear();
        return new Season(today.getMonthValue() >= Month.OCTOBER.getValue() ? year : year - 1);
    }

    public String id() {
        return "%d-%02d".formatted(startYear, (startYear + 1) % 100);
    }

    public Season previous() {
        return new Season(startYear - 1);
    }

    @Override
    public int compareTo(Season other) {
        return Integer.compare(startYear, other.startYear);
    }

    @Override
    public String toString() {
        return id();
    }
}
