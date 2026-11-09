package com.stattracker.ingestion.nba;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/** One row of an {@link NbaResultSet}. Numbers may arrive as JSON numbers or as strings ({@code "240"}). */
public final class NbaRow {

    private final String resultSetName;
    private final Map<String, Integer> columns;
    private final List<@Nullable Object> values;

    NbaRow(String resultSetName, Map<String, Integer> columns, List<@Nullable Object> values) {
        this.resultSetName = resultSetName;
        this.columns = columns;
        this.values = values;
    }

    public String string(String column) {
        String value = optString(column);
        if (value == null) {
            throw missingValue(column);
        }
        return value;
    }

    public @Nullable String optString(String column) {
        Object value = value(column);
        if (value == null) {
            return null;
        }
        String text = value.toString().strip();
        return text.isEmpty() ? null : text;
    }

    public int integer(String column) {
        return Math.toIntExact(longValue(column));
    }

    public @Nullable Integer optInteger(String column) {
        Long value = optLong(column);
        return value == null ? null : Math.toIntExact(value);
    }

    public long longValue(String column) {
        Long value = optLong(column);
        if (value == null) {
            throw missingValue(column);
        }
        return value;
    }

    public @Nullable Long optLong(String column) {
        return switch (value(column)) {
            case null -> null;
            case Long number -> number;
            case Integer number -> number.longValue();
            case Number number when number.doubleValue() == Math.rint(number.doubleValue()) -> number.longValue();
            case String text when text.isBlank() -> null;
            case String text -> parseLong(column, text.strip());
            case Object other -> throw new IllegalStateException(
                    "Column " + column + " of " + resultSetName + " is not an integer: " + other);
        };
    }

    private long parseLong(String column, String text) {
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Column " + column + " of " + resultSetName + " is not an integer: " + text, e);
        }
    }

    private @Nullable Object value(String column) {
        Integer index = columns.get(column);
        if (index == null) {
            throw new IllegalStateException("Column " + column + " missing in result set " + resultSetName);
        }
        return index < values.size() ? values.get(index) : null;
    }

    private IllegalStateException missingValue(String column) {
        return new IllegalStateException("Column " + column + " of " + resultSetName + " is empty in row " + values);
    }
}
