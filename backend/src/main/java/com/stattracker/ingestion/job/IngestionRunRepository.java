package com.stattracker.ingestion.job;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Repository
class IngestionRunRepository {

    private static final TypeReference<Map<String, String>> PARAMS_TYPE = new TypeReference<>() {
    };

    private final JdbcClient jdbc;
    private final JsonMapper jsonMapper;

    IngestionRunRepository(JdbcClient jdbc, JsonMapper jsonMapper) {
        this.jdbc = jdbc;
        this.jsonMapper = jsonMapper;
    }

    long start(IngestionJob job, Map<String, String> params) {
        return jdbc.sql("""
                        INSERT INTO ingestion_run (job, params, status)
                        VALUES (:job, CAST(:params AS jsonb), 'RUNNING')
                        RETURNING id""")
                .param("job", job.name())
                .param("params", jsonMapper.writeValueAsString(params))
                .query(Long.class)
                .single();
    }

    void progress(long id, String step, int rowsUpserted) {
        jdbc.sql("UPDATE ingestion_run SET progress = :step, rows_upserted = :rows WHERE id = :id")
                .param("step", step)
                .param("rows", rowsUpserted)
                .param("id", id)
                .update();
    }

    void finish(long id, RunStatus status, int rowsUpserted, @Nullable String error) {
        jdbc.sql("""
                        UPDATE ingestion_run
                        SET status = :status, finished_at = now(), progress = NULL, rows_upserted = :rows, error = :error
                        WHERE id = :id""")
                .param("status", status.name())
                .param("rows", rowsUpserted)
                .param("error", error)
                .param("id", id)
                .update();
    }

    /** Runs left RUNNING by an application that stopped mid-import. */
    int failInterrupted() {
        return jdbc.sql("""
                        UPDATE ingestion_run
                        SET status = 'FAILED', finished_at = now(), progress = NULL,
                            error = concat_ws(E'\\n', error, 'Interrupted: the application stopped during the import')
                        WHERE status = 'RUNNING'""")
                .update();
    }

    Optional<IngestionRun> find(long id) {
        return jdbc.sql("SELECT * FROM ingestion_run WHERE id = :id").param("id", id).query(this::map).optional();
    }

    List<IngestionRun> recent(int limit) {
        return jdbc.sql("SELECT * FROM ingestion_run ORDER BY id DESC LIMIT :limit")
                .param("limit", limit)
                .query(this::map)
                .list();
    }

    boolean anySucceeded(IngestionJob job) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM ingestion_run WHERE job = :job AND status = 'SUCCESS')")
                .param("job", job.name())
                .query(Boolean.class)
                .single();
    }

    Optional<Instant> lastSuccessAt() {
        return jdbc.sql("""
                        SELECT finished_at FROM ingestion_run
                        WHERE status = 'SUCCESS'
                        ORDER BY finished_at DESC
                        LIMIT 1""")
                .query((rs, rowNum) -> rs.getObject(1, OffsetDateTime.class).toInstant())
                .optional();
    }

    private IngestionRun map(ResultSet rs, int rowNum) throws SQLException {
        return new IngestionRun(
                rs.getLong("id"),
                IngestionJob.valueOf(rs.getString("job")),
                jsonMapper.readValue(rs.getString("params"), PARAMS_TYPE),
                RunStatus.valueOf(rs.getString("status")),
                rs.getString("progress"),
                rs.getObject("started_at", OffsetDateTime.class).toInstant(),
                instant(rs, rs.findColumn("finished_at")),
                rs.getInt("rows_upserted"),
                rs.getString("error"));
    }

    private static @Nullable Instant instant(ResultSet rs, int column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
}
