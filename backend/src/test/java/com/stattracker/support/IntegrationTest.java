package com.stattracker.support;

import java.time.Duration;

import com.stattracker.common.Season;
import com.stattracker.ingestion.job.IngestionService;
import com.stattracker.ingestion.nba.NbaStatsClient;
import org.awaitility.Awaitility;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full application on a real PostgreSQL, with NBA.com replaced by a mock (see {@link NbaFixtures#serve}).
 *
 * <p>The database is a Testcontainers PostgreSQL 18, or — without Docker — an existing database given by
 * {@code TEST_DATABASE_URL} (plus {@code TEST_DATABASE_USERNAME} / {@code TEST_DATABASE_PASSWORD}).
 * Tests truncate its tables, so never point it at the application database.
 */
@SpringBootTest(properties = {
        "stattracker.ingestion.run-on-startup=false",
        "stattracker.ingestion.schedule.enabled=false"})
@AutoConfigureMockMvc
public abstract class IntegrationTest {

    private static @Nullable PostgreSQLContainer container;

    @MockitoBean
    protected NbaStatsClient nbaStatsClient;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcClient jdbc;

    @Autowired
    protected IngestionService ingestionService;

    @DynamicPropertySource
    static synchronized void database(DynamicPropertyRegistry registry) {
        String url = System.getenv("TEST_DATABASE_URL");
        if (url != null && !url.isBlank()) {
            registry.add("spring.datasource.url", () -> url);
            registry.add("spring.datasource.username", () -> env("TEST_DATABASE_USERNAME", "stattracker"));
            registry.add("spring.datasource.password", () -> env("TEST_DATABASE_PASSWORD", "stattracker"));
            return;
        }
        if (container == null) {
            container = new PostgreSQLContainer("postgres:18");
            container.start();
        }
        PostgreSQLContainer postgres = container;
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    /** Empties everything but the teams, which come from a migration. */
    protected void resetDatabase() {
        jdbc.sql("TRUNCATE player_game_stats, team_game_stats, game, season, player, ingestion_run RESTART IDENTITY")
                .update();
    }

    /** Fresh database with the 2025-26 fixtures, imported the way the application does it: as a backfill job. */
    protected void importFixtureSeason() {
        resetDatabase();
        NbaFixtures.serve(nbaStatsClient);
        ingestionService.startBackfill(new Season(2025));
        awaitIdle();
        assertThat(jdbc.sql("SELECT status FROM ingestion_run ORDER BY id DESC LIMIT 1").query(String.class).single())
                .isEqualTo("SUCCESS");
    }

    protected void awaitIdle() {
        Awaitility.await().atMost(Duration.ofSeconds(30)).until(() -> !ingestionService.isRunning());
    }

    protected long count(String tableAndCondition) {
        return jdbc.sql("SELECT count(*) FROM " + tableAndCondition).query(Long.class).single();
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
