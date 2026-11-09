package com.stattracker.ingestion.job;

import java.time.ZoneId;

import com.stattracker.common.Season;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

@Component
class IngestionScheduler {

    private static final Logger log = LoggerFactory.getLogger(IngestionScheduler.class);

    private final IngestionService ingestion;
    private final IngestionRunRepository runs;
    private final IngestionProperties properties;

    IngestionScheduler(IngestionService ingestion, IngestionRunRepository runs, IngestionProperties properties) {
        this.ingestion = ingestion;
        this.runs = runs;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    void onStartup() {
        int interrupted = runs.failInterrupted();
        if (interrupted > 0) {
            log.warn("Marked {} import run(s) interrupted by the previous shutdown as FAILED", interrupted);
        }
        if (!properties.runOnStartup()) {
            return;
        }
        if (runs.anySucceeded(IngestionJob.BACKFILL)) {
            startSync("Startup");
        } else {
            log.info("No completed history import yet, importing seasons since {}", properties.backfillFrom());
            ingestion.startBackfill(Season.parse(properties.backfillFrom()));
        }
    }

    void startSync(String trigger) {
        try {
            ingestion.startDailySync();
        } catch (IngestionInProgressException e) {
            log.info("{} sync skipped: another import is running", trigger);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableScheduling
    @ConditionalOnProperty(prefix = "stattracker.ingestion.schedule", name = "enabled", havingValue = "true",
            matchIfMissing = true)
    static class ScheduleConfig implements SchedulingConfigurer {

        private final IngestionScheduler scheduler;
        private final IngestionProperties properties;

        ScheduleConfig(IngestionScheduler scheduler, IngestionProperties properties) {
            this.scheduler = scheduler;
            this.properties = properties;
        }

        @Override
        public void configureTasks(ScheduledTaskRegistrar registrar) {
            IngestionProperties.Schedule schedule = properties.schedule();
            registrar.addCronTask(new CronTask(() -> scheduler.startSync("Scheduled"),
                    new CronTrigger(schedule.cron(), ZoneId.of(schedule.zone()))));
        }
    }
}
