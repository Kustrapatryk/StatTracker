package com.stattracker.ingestion.job;

import java.time.LocalDate;

record GameRow(
        String id,
        LocalDate date,
        long homeTeamId,
        long awayTeamId,
        int homeScore,
        int awayScore,
        boolean neutralSite) {
}
