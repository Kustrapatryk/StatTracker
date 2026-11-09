package com.stattracker.ingestion.job;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param backfillFrom first season imported when the database is empty
 * @param runOnStartup on startup import the whole history if it has never been imported, otherwise sync
 */
@ConfigurationProperties("stattracker.ingestion")
public record IngestionProperties(
        @DefaultValue("2003-04") String backfillFrom,
        @DefaultValue("true") boolean runOnStartup,
        @DefaultValue Schedule schedule) {

    public record Schedule(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("0 0 9,15 * * *") String cron,
            @DefaultValue("Europe/Warsaw") String zone) {
    }
}
