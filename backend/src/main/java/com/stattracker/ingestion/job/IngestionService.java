package com.stattracker.ingestion.job;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

import com.stattracker.common.Season;
import com.stattracker.common.SeasonType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

/**
 * Starts import jobs in the background, one at a time. A failed step (e.g. one season after all retries) is
 * recorded and the job moves on, so a single NBA.com hiccup does not throw away the rest of a backfill. Several
 * failed steps in a row abort the job instead: NBA.com is then down or blocking us, and every further step would
 * only wait for its timeouts.
 */
@Service
public class IngestionService {

    /** Older game logs miss stats that the schema requires; NBA.com's full box score era starts here. */
    public static final Season FIRST_SUPPORTED_SEASON = new Season(1996);

    static final int MAX_FAILED_STEPS_IN_A_ROW = 3;

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final SeasonImporter importer;
    private final IngestionRunRepository runs;
    private final TaskExecutor executor;
    private final Clock clock;
    private final AtomicBoolean running = new AtomicBoolean();

    IngestionService(
            SeasonImporter importer,
            IngestionRunRepository runs,
            @Qualifier("applicationTaskExecutor") TaskExecutor executor,
            Clock clock) {
        this.importer = importer;
        this.runs = runs;
        this.executor = executor;
        this.clock = clock;
    }

    public boolean isRunning() {
        return running.get();
    }

    /** Players, then every season from the current one back to {@code from}, so recent data is available first. */
    public IngestionRun startBackfill(Season from) {
        Season to = Season.current(clock);
        if (from.compareTo(FIRST_SUPPORTED_SEASON) < 0 || from.compareTo(to) > 0) {
            throw new IllegalArgumentException(
                    "Backfill must start between " + FIRST_SUPPORTED_SEASON + " and " + to + ", got " + from);
        }
        return start(IngestionJob.BACKFILL, Map.of("from", from.id(), "to", to.id()), run -> {
            run.step("players", () -> importer.importPlayers(to));
            for (Season season = to; season.compareTo(from) >= 0; season = season.previous()) {
                importSeason(run, season);
            }
        });
    }

    /** Re-imports the whole current season, which also picks up stat corrections made by the NBA. */
    public IngestionRun startDailySync() {
        Season current = Season.current(clock);
        return start(IngestionJob.DAILY_SYNC, Map.of("season", current.id()), run -> {
            run.step("players", () -> importer.importPlayers(current));
            importSeason(run, current);
        });
    }

    private void importSeason(Run run, Season season) {
        for (SeasonType type : SeasonType.values()) {
            run.step(season + " " + type, () -> importer.importSeason(season, type));
        }
    }

    private IngestionRun start(IngestionJob job, Map<String, String> params, Consumer<Run> steps) {
        if (!running.compareAndSet(false, true)) {
            throw new IngestionInProgressException();
        }
        try {
            long runId = runs.start(job, params);
            log.info("{} #{} started {}", job, runId, params);
            executor.execute(() -> execute(new Run(runId, job), steps));
            return runs.find(runId).orElseThrow();
        } catch (RuntimeException e) {
            running.set(false);
            throw e;
        }
    }

    private void execute(Run run, Consumer<Run> steps) {
        try {
            steps.accept(run);
        } catch (AbortedException e) {
            String reason = "Aborted after " + MAX_FAILED_STEPS_IN_A_ROW
                    + " failed steps in a row, NBA.com is unreachable or blocking requests";
            log.warn("{} #{}: {}", run.job, run.id, reason);
            run.failures.add(reason);
        } catch (RuntimeException e) {
            run.fail("job", e);
        } finally {
            try {
                RunStatus status = run.failures.isEmpty() ? RunStatus.SUCCESS : RunStatus.FAILED;
                runs.finish(run.id, status, run.rows, run.failures.isEmpty() ? null : String.join("\n", run.failures));
                log.info("{} #{} finished {}: {} rows in {} s", run.job, run.id, status, run.rows,
                        Duration.between(run.startedAt, Instant.now()).toSeconds());
            } finally {
                running.set(false);
            }
        }
    }

    private final class Run {

        private final long id;
        private final IngestionJob job;
        private final Instant startedAt = Instant.now();
        private final List<String> failures = new ArrayList<>();
        private int rows;
        private int failedStepsInARow;

        private Run(long id, IngestionJob job) {
            this.id = id;
            this.job = job;
        }

        void step(String name, IntSupplier action) {
            runs.progress(id, name, rows);
            try {
                rows += action.getAsInt();
                failedStepsInARow = 0;
            } catch (RuntimeException e) {
                fail(name, e);
                if (++failedStepsInARow == MAX_FAILED_STEPS_IN_A_ROW) {
                    throw new AbortedException();
                }
            }
        }

        void fail(String step, RuntimeException e) {
            log.error("{} #{}: step {} failed", job, id, step, e);
            failures.add(step + ": " + e.getMessage());
        }
    }

    private static final class AbortedException extends RuntimeException {

        AbortedException() {
            super(null, null, false, false);
        }
    }
}
