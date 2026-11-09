package com.stattracker.ingestion.nba;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jspecify.annotations.Nullable;

/** Envelope of stats.nba.com responses: named tables of {@code headers} and {@code rowSet}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NbaResponse(List<NbaResultSet> resultSets) {

    public NbaResponse(@Nullable List<NbaResultSet> resultSets) {
        this.resultSets = resultSets == null ? List.of() : List.copyOf(resultSets);
    }

    public NbaResultSet resultSet(String name) {
        return resultSets.stream()
                .filter(resultSet -> resultSet.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Result set " + name + " missing, got "
                        + resultSets.stream().map(NbaResultSet::name).toList()));
    }
}
