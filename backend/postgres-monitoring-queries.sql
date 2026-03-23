-- postgres-monitoring-queries.sql
-- Consultas operacionais para PostgreSQL 14+
-- Ajuste limites e filtros conforme o ambiente.

-- ============================================================================
-- 1. Locks ativos
-- Lista locks atuais com detalhes da sessao, relacao e tempos de execucao.
-- ============================================================================
SELECT DISTINCT
    a.pid,
    a.usename AS user_name,
    a.datname AS database_name,
    a.state,
    l.locktype,
    l.mode,
    l.granted,
    COALESCE(c.relname, l.relation::regclass::text) AS relation_name,
    a.query,
    a.query_start,
    a.xact_start,
    now() - a.query_start AS query_duration,
    now() - a.xact_start AS transaction_duration,
    a.application_name,
    a.client_addr,
    a.wait_event_type,
    a.wait_event
FROM pg_stat_activity a
LEFT JOIN pg_locks l ON l.pid = a.pid
LEFT JOIN pg_class c ON c.oid = l.relation
WHERE a.datname = current_database()
ORDER BY a.query_start NULLS LAST, a.pid;

-- ============================================================================
-- 2. Sessoes bloqueadas e bloqueadoras
-- Relaciona a sessao bloqueada com a sessao que esta causando o bloqueio.
-- ============================================================================
WITH blocked_sessions AS (
    SELECT
        a.pid AS blocked_pid,
        a.usename AS blocked_user,
        a.datname AS blocked_database,
        a.application_name AS blocked_application_name,
        a.client_addr AS blocked_client_addr,
        a.query AS blocked_query,
        a.query_start AS blocked_query_start,
        a.xact_start AS blocked_xact_start,
        unnest(pg_blocking_pids(a.pid)) AS blocking_pid
    FROM pg_stat_activity a
    WHERE cardinality(pg_blocking_pids(a.pid)) > 0
)
SELECT
    b.blocked_pid,
    b.blocked_user,
    b.blocked_database,
    b.blocked_application_name,
    b.blocked_client_addr,
    b.blocked_query,
    b.blocked_query_start,
    now() - b.blocked_query_start AS blocked_query_duration,
    b.blocking_pid,
    blocker.usename AS blocking_user,
    blocker.datname AS blocking_database,
    blocker.application_name AS blocking_application_name,
    blocker.client_addr AS blocking_client_addr,
    blocker.state AS blocking_state,
    blocker.query AS blocking_query,
    blocker.query_start AS blocking_query_start,
    now() - blocker.query_start AS blocking_query_duration
FROM blocked_sessions b
JOIN pg_stat_activity blocker ON blocker.pid = b.blocking_pid
ORDER BY blocked_query_duration DESC NULLS LAST;

-- ============================================================================
-- 3. Queries em execucao ha mais de X segundos
-- Ajuste o valor 30 conforme a necessidade.
-- ============================================================================
SELECT
    pid,
    usename AS user_name,
    datname AS database_name,
    state,
    wait_event_type,
    wait_event,
    application_name,
    client_addr,
    query_start,
    now() - query_start AS query_duration,
    query
FROM pg_stat_activity
WHERE state = 'active'
  AND query_start IS NOT NULL
  AND now() - query_start > interval '30 seconds'
  AND pid <> pg_backend_pid()
ORDER BY query_duration DESC;

-- ============================================================================
-- 4. Conexoes por estado
-- Visao consolidada do uso de conexoes por estado.
-- ============================================================================
SELECT
    COALESCE(state, 'unknown') AS connection_state,
    COUNT(*) AS total_connections
FROM pg_stat_activity
WHERE datname = current_database()
GROUP BY COALESCE(state, 'unknown')
ORDER BY total_connections DESC, connection_state;

-- ============================================================================
-- 5. Conexoes por usuario
-- ============================================================================
SELECT
    usename AS user_name,
    COUNT(*) AS total_connections
FROM pg_stat_activity
WHERE datname = current_database()
GROUP BY usename
ORDER BY total_connections DESC, user_name;

-- ============================================================================
-- 6. Conexoes por application_name
-- ============================================================================
SELECT
    COALESCE(NULLIF(application_name, ''), 'unknown') AS application_name,
    COUNT(*) AS total_connections
FROM pg_stat_activity
WHERE datname = current_database()
GROUP BY COALESCE(NULLIF(application_name, ''), 'unknown')
ORDER BY total_connections DESC, application_name;

-- ============================================================================
-- 7. Top queries por quantidade de execucoes
-- Requer pg_stat_statements habilitado.
-- ============================================================================
SELECT
    query,
    calls,
    round(total_exec_time::numeric, 2) AS total_exec_time_ms,
    round(mean_exec_time::numeric, 2) AS mean_exec_time_ms,
    rows,
    shared_blks_hit,
    shared_blks_read,
    temp_blks_written
FROM pg_stat_statements
WHERE dbid = (SELECT oid FROM pg_database WHERE datname = current_database())
ORDER BY calls DESC
LIMIT 20;

-- ============================================================================
-- 8. Top queries por tempo medio
-- Requer pg_stat_statements habilitado.
-- ============================================================================
SELECT
    query,
    calls,
    round(mean_exec_time::numeric, 2) AS mean_exec_time_ms,
    round(total_exec_time::numeric, 2) AS total_exec_time_ms,
    rows,
    shared_blks_hit,
    shared_blks_read,
    temp_blks_written
FROM pg_stat_statements
WHERE dbid = (SELECT oid FROM pg_database WHERE datname = current_database())
ORDER BY mean_exec_time DESC
LIMIT 20;

-- ============================================================================
-- 9. Top queries por tempo total
-- Requer pg_stat_statements habilitado.
-- ============================================================================
SELECT
    query,
    calls,
    round(total_exec_time::numeric, 2) AS total_exec_time_ms,
    round(mean_exec_time::numeric, 2) AS mean_exec_time_ms,
    rows,
    shared_blks_hit,
    shared_blks_read,
    temp_blks_written
FROM pg_stat_statements
WHERE dbid = (SELECT oid FROM pg_database WHERE datname = current_database())
ORDER BY total_exec_time DESC
LIMIT 20;

-- ============================================================================
-- 10. Idle in transaction
-- Ajuste o intervalo conforme a necessidade.
-- ============================================================================
SELECT
    pid,
    usename AS user_name,
    datname AS database_name,
    application_name,
    client_addr,
    xact_start,
    now() - xact_start AS idle_in_transaction_duration,
    state,
    wait_event_type,
    wait_event,
    query
FROM pg_stat_activity
WHERE state = 'idle in transaction'
  AND xact_start IS NOT NULL
  AND now() - xact_start > interval '60 seconds'
ORDER BY idle_in_transaction_duration DESC;

-- ============================================================================
-- 11. Tamanho das tabelas
-- ============================================================================
SELECT
    schemaname AS schema_name,
    relname AS table_name,
    pg_size_pretty(pg_relation_size(relid)) AS table_size,
    pg_relation_size(relid) AS table_size_bytes,
    pg_size_pretty(pg_total_relation_size(relid)) AS total_size,
    pg_total_relation_size(relid) AS total_size_bytes,
    COALESCE(n_live_tup, 0) AS estimated_live_rows
FROM pg_stat_user_tables
ORDER BY pg_total_relation_size(relid) DESC;

-- ============================================================================
-- 12. Tamanho dos indices
-- ============================================================================
SELECT
    schemaname AS schema_name,
    relname AS table_name,
    pg_size_pretty(pg_indexes_size(relid)) AS indexes_size,
    pg_indexes_size(relid) AS indexes_size_bytes
FROM pg_stat_user_tables
ORDER BY pg_indexes_size(relid) DESC;

-- ============================================================================
-- 13. Cache hit ratio
-- Percentual geral de acerto de cache nas tabelas de usuario.
-- ============================================================================
SELECT
    round(
        CASE
            WHEN SUM(heap_blks_hit + heap_blks_read) = 0 THEN 0
            ELSE (SUM(heap_blks_hit)::numeric / SUM(heap_blks_hit + heap_blks_read)) * 100
        END,
        2
    ) AS cache_hit_ratio_percent,
    COALESCE(SUM(heap_blks_hit), 0) AS heap_blks_hit,
    COALESCE(SUM(heap_blks_read), 0) AS heap_blks_read
FROM pg_statio_user_tables;

-- ============================================================================
-- 14. Dead tuples
-- Ajuda a identificar necessidade de vacuum e bloat potencial.
-- ============================================================================
SELECT
    schemaname AS schema_name,
    relname AS table_name,
    n_live_tup AS live_tuples,
    n_dead_tup AS dead_tuples,
    round(
        CASE
            WHEN n_live_tup = 0 THEN 0
            ELSE (n_dead_tup::numeric / n_live_tup) * 100
        END,
        2
    ) AS dead_tuple_percent,
    last_vacuum,
    last_autovacuum,
    last_analyze,
    last_autoanalyze
FROM pg_stat_user_tables
ORDER BY n_dead_tup DESC, dead_tuple_percent DESC;

-- ============================================================================
-- 15. Sessoes mais antigas
-- Ordena pelas transacoes mais antigas e depois pelo inicio da query.
-- ============================================================================
SELECT
    pid,
    usename AS user_name,
    datname AS database_name,
    application_name,
    client_addr,
    backend_start,
    xact_start,
    query_start,
    state,
    now() - backend_start AS session_age,
    now() - xact_start AS transaction_age,
    now() - query_start AS query_age,
    wait_event_type,
    wait_event,
    query
FROM pg_stat_activity
WHERE datname = current_database()
ORDER BY xact_start NULLS LAST, query_start NULLS LAST, backend_start;
