package com.stattracker.ingestion.job;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.stattracker.support.IntegrationTest;
import com.stattracker.support.NbaFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.client.ResourceAccessException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IngestionAdminApiTest extends IntegrationTest {

    @BeforeEach
    void setUp() {
        resetDatabase();
        NbaFixtures.serve(nbaStatsClient);
    }

    @Test
    void backfillRunsInBackgroundAndReportsItsResult() throws Exception {
        mockMvc.perform(post("/api/admin/ingestion/backfill").param("from", "2025-26"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.job").value("BACKFILL"))
                .andExpect(jsonPath("$.params.from").value("2025-26"));
        awaitIdle();

        mockMvc.perform(get("/api/admin/ingestion/runs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$[0].rowsUpserted").value(greaterThan(0)))
                .andExpect(jsonPath("$[0].progress").value((Object) null))
                .andExpect(jsonPath("$[0].finishedAt").isNotEmpty());
        mockMvc.perform(get("/api/data-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latestSeason").value("2025-26"))
                .andExpect(jsonPath("$.games").value(137 + 1 + 33))
                .andExpect(jsonPath("$.importRunning").value(false))
                .andExpect(jsonPath("$.lastSuccessAt").isNotEmpty())
                .andExpect(jsonPath("$.lastRun.job").value("BACKFILL"));
    }

    @Test
    void rejectsASecondImportWhileOneIsRunning() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        when(nbaStatsClient.fetch(eq("playerindex"), anyMap())).thenAnswer(invocation -> {
            release.await(10, TimeUnit.SECONDS);
            return NbaFixtures.load("playerindex-historical.json");
        });

        mockMvc.perform(post("/api/admin/ingestion/sync")).andExpect(status().isAccepted());
        mockMvc.perform(post("/api/admin/ingestion/backfill"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Another import is already running"));
        mockMvc.perform(get("/api/data-status"))
                .andExpect(jsonPath("$.importRunning").value(true))
                .andExpect(jsonPath("$.lastRun.status").value("RUNNING"))
                .andExpect(jsonPath("$.lastRun.progress").value("players"));

        release.countDown();
        awaitIdle();
        mockMvc.perform(get("/api/admin/ingestion/runs"))
                .andExpect(jsonPath("$[0].job").value("DAILY_SYNC"))
                .andExpect(jsonPath("$[0].status").value("SUCCESS"));
    }

    @Test
    void recordsAFailedStepAndImportsTheRest() throws Exception {
        when(nbaStatsClient.fetch(eq("playerindex"), anyMap())).thenThrow(new ResourceAccessException("Read timed out"));

        mockMvc.perform(post("/api/admin/ingestion/backfill").param("from", "2025-26"))
                .andExpect(status().isAccepted());
        awaitIdle();

        mockMvc.perform(get("/api/admin/ingestion/runs"))
                .andExpect(jsonPath("$[0].status").value("FAILED"))
                .andExpect(jsonPath("$[0].error").value(containsString("players: Read timed out")));
        mockMvc.perform(get("/api/data-status")).andExpect(jsonPath("$.games").value(137 + 1 + 33));
    }

    @Test
    void abortsWhenNbaComKeepsFailing() throws Exception {
        when(nbaStatsClient.fetch(anyString(), anyMap())).thenThrow(new ResourceAccessException("Connection reset"));

        mockMvc.perform(post("/api/admin/ingestion/backfill").param("from", "2003-04"))
                .andExpect(status().isAccepted());
        awaitIdle();

        verify(nbaStatsClient, times(IngestionService.MAX_FAILED_STEPS_IN_A_ROW)).fetch(anyString(), anyMap());
        mockMvc.perform(get("/api/admin/ingestion/runs"))
                .andExpect(jsonPath("$[0].status").value("FAILED"))
                .andExpect(jsonPath("$[0].error").value(containsString("players: Connection reset")))
                .andExpect(jsonPath("$[0].error").value(containsString("Aborted after 3 failed steps in a row")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1990-91", "2090-91", "2025", "2025-27"})
    void rejectsInvalidBackfillStart(String from) throws Exception {
        mockMvc.perform(post("/api/admin/ingestion/backfill").param("from", from))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
