package com.stattracker.ingestion.nba;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jspecify.annotations.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NbaResultSet(String name, List<String> headers, List<List<@Nullable Object>> rowSet) {

    /** Rows with values looked up by column name, so a reordered or extended response still parses. */
    public List<NbaRow> rows() {
        Map<String, Integer> columns = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            columns.put(headers.get(i), i);
        }
        return rowSet.stream().map(values -> new NbaRow(name, columns, values)).toList();
    }
}
