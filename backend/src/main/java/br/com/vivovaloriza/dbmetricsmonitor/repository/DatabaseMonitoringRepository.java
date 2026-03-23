package br.com.vivovaloriza.dbmetricsmonitor.repository;

import br.com.vivovaloriza.dbmetricsmonitor.dto.CacheHitRatioResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.DatabaseSettingResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.GroupedConnectionResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.IdleSessionResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.LockInfoResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.RunningQueryResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.TableAccessResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.TableSizeResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.TopQueryResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.VacuumHealthResponse;
import br.com.vivovaloriza.dbmetricsmonitor.exception.DatabaseOperationException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DatabaseMonitoringRepository {

    private static final String BASE_LOCKS_QUERY = """
            WITH lock_details AS (
                SELECT DISTINCT
                    a.pid,
                    a.usename,
                    a.datname,
                    a.state,
                    l.locktype,
                    COALESCE(c.relname, l.relation::regclass::text) AS relation_name,
                    a.query,
                    a.query_start,
                    a.xact_start,
                    EXTRACT(EPOCH FROM (now() - a.query_start))::bigint AS query_duration_seconds,
                    EXTRACT(EPOCH FROM (now() - a.xact_start))::bigint AS transaction_duration_seconds,
                    pg_blocking_pids(a.pid) AS blocking_pids,
                    a.application_name,
                    a.client_addr::text AS client_addr,
                    a.wait_event_type,
                    a.wait_event
                FROM pg_stat_activity a
                LEFT JOIN pg_locks l ON l.pid = a.pid
                LEFT JOIN pg_class c ON c.oid = l.relation
                WHERE a.datname = current_database()
            )
            SELECT
                ld.pid,
                ld.usename,
                ld.datname,
                ld.state,
                ld.locktype,
                ld.relation_name,
                ld.query,
                ld.query_start,
                ld.xact_start,
                ld.query_duration_seconds,
                ld.transaction_duration_seconds,
                ld.blocking_pids[1] AS blocked_by_pid,
                blocker.query AS blocked_by_query,
                ld.application_name,
                ld.client_addr,
                ld.wait_event_type,
                ld.wait_event
            FROM lock_details ld
            LEFT JOIN pg_stat_activity blocker ON blocker.pid = ld.blocking_pids[1]
            """;

    private static final String TOP_QUERY_BASE = """
            SELECT
                query,
                calls,
                round(total_exec_time::numeric, 2) AS total_exec_time,
                round(mean_exec_time::numeric, 2) AS mean_exec_time,
                rows,
                shared_blks_hit,
                shared_blks_read,
                temp_blks_written
            FROM pg_stat_statements
            WHERE dbid = (SELECT oid FROM pg_database WHERE datname = current_database())
              AND query NOT ILIKE '%%pg_stat_statements%%'
            """;

    private final JdbcTemplate jdbcTemplate;

    public boolean isDatabaseUp() {
        try {
            Integer value = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return value != null && value == 1;
        } catch (DataAccessException ex) {
            return false;
        }
    }

    public long getCurrentBackendPid() {
        return Optional.ofNullable(jdbcTemplate.queryForObject("SELECT pg_backend_pid()", Long.class)).orElse(-1L);
    }

    public List<LockInfoResponse> findAllLocks() {
        return queryLocks(BASE_LOCKS_QUERY + " ORDER BY ld.query_start NULLS LAST");
    }

    public List<LockInfoResponse> findBlockedLocks() {
        return queryLocks(BASE_LOCKS_QUERY + " WHERE cardinality(ld.blocking_pids) > 0 ORDER BY ld.query_duration_seconds DESC NULLS LAST");
    }

    public List<LockInfoResponse> findBlockingLocks() {
        String sql = BASE_LOCKS_QUERY + """
                 WHERE EXISTS (
                    SELECT 1
                    FROM pg_stat_activity blocked
                    WHERE ld.pid = ANY(pg_blocking_pids(blocked.pid))
                 )
                 ORDER BY ld.query_duration_seconds DESC NULLS LAST
                """;
        return queryLocks(sql);
    }

    public List<RunningQueryResponse> findRunningQueriesAboveSeconds(long minDurationSeconds) {
        String sql = """
                SELECT
                    pid,
                    usename,
                    datname,
                    EXTRACT(EPOCH FROM (now() - query_start))::bigint AS duration_seconds,
                    state,
                    COALESCE(wait_event_type || ':' || wait_event, wait_event) AS wait_event,
                    query
                FROM pg_stat_activity
                WHERE state = 'active'
                  AND pid <> pg_backend_pid()
                  AND query_start IS NOT NULL
                  AND EXTRACT(EPOCH FROM (now() - query_start)) >= ?
                ORDER BY duration_seconds DESC
                """;
        return jdbcTemplate.query(sql, runningQueryMapper(), minDurationSeconds);
    }

    public Map<String, Object> findConnectionCounters() {
        String sql = """
                SELECT
                    COUNT(*)::int AS total_connections,
                    COUNT(*) FILTER (WHERE state = 'active')::int AS active_connections,
                    COUNT(*) FILTER (WHERE state = 'idle')::int AS idle_connections,
                    COUNT(*) FILTER (WHERE state = 'idle in transaction')::int AS idle_in_transaction_connections,
                    current_setting('max_connections')::int AS max_connections
                FROM pg_stat_activity
                WHERE datname = current_database()
                """;
        return jdbcTemplate.queryForMap(sql);
    }

    public List<GroupedConnectionResponse> findConnectionsByUser() {
        return jdbcTemplate.query("""
                SELECT usename AS name, COUNT(*)::int AS connections
                FROM pg_stat_activity
                WHERE datname = current_database()
                GROUP BY usename
                ORDER BY connections DESC, usename
                """, groupedConnectionMapper());
    }

    public List<GroupedConnectionResponse> findConnectionsByApplication() {
        return jdbcTemplate.query("""
                SELECT COALESCE(NULLIF(application_name, ''), 'unknown') AS name, COUNT(*)::int AS connections
                FROM pg_stat_activity
                WHERE datname = current_database()
                GROUP BY COALESCE(NULLIF(application_name, ''), 'unknown')
                ORDER BY connections DESC, name
                """, groupedConnectionMapper());
    }

    public boolean isPgStatStatementsAvailable() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM pg_extension
                WHERE extname = 'pg_stat_statements'
                """, Integer.class);
        return count != null && count > 0;
    }

    public List<TopQueryResponse> findTopQueries(int limit) {
        return jdbcTemplate.query(TOP_QUERY_BASE + " ORDER BY calls DESC LIMIT ?", topQueryMapper(), limit);
    }

    public List<TopQueryResponse> findSlowQueries(int limit) {
        return jdbcTemplate.query(TOP_QUERY_BASE + " ORDER BY total_exec_time DESC LIMIT ?", topQueryMapper(), limit);
    }

    public List<TopQueryResponse> findQueriesByMeanTime(int limit) {
        return jdbcTemplate.query(TOP_QUERY_BASE + " ORDER BY mean_exec_time DESC LIMIT ?", topQueryMapper(), limit);
    }

    public boolean sessionExists(long pid) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pg_stat_activity WHERE pid = ?", Integer.class, pid);
        return count != null && count > 0;
    }

    public String findSessionApplicationName(long pid) {
        return jdbcTemplate.query("""
                SELECT application_name
                FROM pg_stat_activity
                WHERE pid = ?
                """, rs -> rs.next() ? rs.getString("application_name") : null, pid);
    }

    public boolean terminateSession(long pid) {
        Boolean terminated = jdbcTemplate.queryForObject("SELECT pg_terminate_backend(?)", Boolean.class, pid);
        return Boolean.TRUE.equals(terminated);
    }

    public List<TableSizeResponse> findTopTableSizes(int limit) {
        return jdbcTemplate.query("""
                SELECT
                    schemaname,
                    relname,
                    pg_size_pretty(pg_total_relation_size(relid)) AS total_size,
                    pg_size_pretty(pg_relation_size(relid)) AS table_size,
                    pg_size_pretty(pg_indexes_size(relid)) AS indexes_size,
                    COALESCE(n_live_tup, 0)::bigint AS estimated_rows
                FROM pg_stat_user_tables
                ORDER BY pg_total_relation_size(relid) DESC
                LIMIT ?
                """, (rs, rowNum) -> new TableSizeResponse(
                rs.getString("schemaname"),
                rs.getString("relname"),
                rs.getString("total_size"),
                rs.getString("table_size"),
                rs.getString("indexes_size"),
                rs.getLong("estimated_rows")
        ), limit);
    }

    public List<TableAccessResponse> findTopTableAccesses(int limit) {
        return jdbcTemplate.query("""
                SELECT
                    schemaname,
                    relname,
                    seq_scan,
                    idx_scan,
                    (seq_tup_read + idx_tup_fetch)::bigint AS total_reads,
                    (n_tup_ins + n_tup_upd + n_tup_del)::bigint AS total_writes,
                    n_live_tup
                FROM pg_stat_user_tables
                ORDER BY (seq_scan + idx_scan) DESC
                LIMIT ?
                """, (rs, rowNum) -> new TableAccessResponse(
                rs.getString("schemaname"),
                rs.getString("relname"),
                rs.getLong("seq_scan"),
                rs.getLong("idx_scan"),
                rs.getLong("total_reads"),
                rs.getLong("total_writes"),
                rs.getLong("n_live_tup")
        ), limit);
    }

    public List<VacuumHealthResponse> findVacuumHealth(int limit) {
        return jdbcTemplate.query("""
                SELECT
                    schemaname,
                    relname,
                    n_live_tup,
                    n_dead_tup,
                    CASE
                        WHEN n_live_tup = 0 THEN 0
                        ELSE round((n_dead_tup::numeric / NULLIF(n_live_tup, 0)) * 100, 2)
                    END AS dead_tuple_percent,
                    last_vacuum,
                    last_autovacuum,
                    last_analyze,
                    last_autoanalyze
                FROM pg_stat_user_tables
                ORDER BY n_dead_tup DESC
                LIMIT ?
                """, (rs, rowNum) -> new VacuumHealthResponse(
                rs.getString("schemaname"),
                rs.getString("relname"),
                rs.getLong("n_live_tup"),
                rs.getLong("n_dead_tup"),
                rs.getBigDecimal("dead_tuple_percent"),
                getOffsetDateTime(rs, "last_vacuum"),
                getOffsetDateTime(rs, "last_autovacuum"),
                getOffsetDateTime(rs, "last_analyze"),
                getOffsetDateTime(rs, "last_autoanalyze")
        ), limit);
    }

    public CacheHitRatioResponse findCacheHitRatio() {
        return jdbcTemplate.queryForObject("""
                SELECT
                    round(
                        CASE
                            WHEN SUM(heap_blks_hit + heap_blks_read) = 0 THEN 0
                            ELSE (SUM(heap_blks_hit)::numeric / SUM(heap_blks_hit + heap_blks_read)) * 100
                        END, 2
                    ) AS cache_hit_ratio_percent,
                    COALESCE(SUM(heap_blks_read), 0)::bigint AS heap_blks_read,
                    COALESCE(SUM(heap_blks_hit), 0)::bigint AS heap_blks_hit
                FROM pg_statio_user_tables
                """, (rs, rowNum) -> new CacheHitRatioResponse(
                rs.getBigDecimal("cache_hit_ratio_percent"),
                rs.getLong("heap_blks_read"),
                rs.getLong("heap_blks_hit")
        ));
    }

    public List<IdleSessionResponse> findIdleInTransactionSessions(long minDurationSeconds) {
        return jdbcTemplate.query("""
                SELECT
                    pid,
                    usename,
                    datname,
                    EXTRACT(EPOCH FROM (now() - xact_start))::bigint AS duration_seconds,
                    xact_start,
                    query,
                    application_name,
                    client_addr::text AS client_addr
                FROM pg_stat_activity
                WHERE state = 'idle in transaction'
                  AND xact_start IS NOT NULL
                  AND EXTRACT(EPOCH FROM (now() - xact_start)) >= ?
                ORDER BY duration_seconds DESC
                """, (rs, rowNum) -> new IdleSessionResponse(
                rs.getLong("pid"),
                rs.getString("usename"),
                rs.getString("datname"),
                Duration.ofSeconds(rs.getLong("duration_seconds")),
                getOffsetDateTime(rs, "xact_start"),
                rs.getString("query"),
                rs.getString("application_name"),
                rs.getString("client_addr")
        ), minDurationSeconds);
    }

    public List<DatabaseSettingResponse> findDatabaseSettings() {
        return jdbcTemplate.query("""
                SELECT name, setting, unit
                FROM pg_settings
                WHERE name IN (
                    'shared_buffers',
                    'work_mem',
                    'maintenance_work_mem',
                    'effective_cache_size',
                    'max_connections'
                )
                ORDER BY name
                """, (rs, rowNum) -> new DatabaseSettingResponse(
                rs.getString("name"),
                rs.getString("setting"),
                rs.getString("unit")
        ));
    }

    private List<LockInfoResponse> queryLocks(String sql) {
        try {
            return jdbcTemplate.query(sql, lockInfoMapper());
        } catch (DataAccessException ex) {
            throw new DatabaseOperationException("Falha ao consultar locks do PostgreSQL.", ex);
        }
    }

    private RowMapper<LockInfoResponse> lockInfoMapper() {
        return (rs, rowNum) -> new LockInfoResponse(
                rs.getLong("pid"),
                rs.getString("usename"),
                rs.getString("datname"),
                rs.getString("state"),
                rs.getString("locktype"),
                rs.getString("relation_name"),
                rs.getString("query"),
                getOffsetDateTime(rs, "query_start"),
                getOffsetDateTime(rs, "xact_start"),
                durationOf(rs, "query_duration_seconds"),
                durationOf(rs, "transaction_duration_seconds"),
                getNullableLong(rs, "blocked_by_pid"),
                rs.getString("blocked_by_query"),
                rs.getString("application_name"),
                rs.getString("client_addr"),
                rs.getString("wait_event_type"),
                rs.getString("wait_event")
        );
    }

    private RowMapper<RunningQueryResponse> runningQueryMapper() {
        return (rs, rowNum) -> new RunningQueryResponse(
                rs.getLong("pid"),
                rs.getString("usename"),
                rs.getString("datname"),
                Duration.ofSeconds(rs.getLong("duration_seconds")),
                rs.getString("state"),
                rs.getString("wait_event"),
                rs.getString("query")
        );
    }

    private RowMapper<GroupedConnectionResponse> groupedConnectionMapper() {
        return (rs, rowNum) -> new GroupedConnectionResponse(
                rs.getString("name"),
                rs.getInt("connections")
        );
    }

    private RowMapper<TopQueryResponse> topQueryMapper() {
        return (rs, rowNum) -> new TopQueryResponse(
                rs.getString("query"),
                rs.getLong("calls"),
                rs.getBigDecimal("total_exec_time"),
                rs.getBigDecimal("mean_exec_time"),
                rs.getLong("rows"),
                rs.getLong("shared_blks_hit"),
                rs.getLong("shared_blks_read"),
                rs.getLong("temp_blks_written")
        );
    }

    private static Duration durationOf(ResultSet rs, String column) throws SQLException {
        long seconds = rs.getLong(column);
        return rs.wasNull() ? null : Duration.ofSeconds(seconds);
    }

    private static Long getNullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private static OffsetDateTime getOffsetDateTime(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, OffsetDateTime.class);
    }
}
