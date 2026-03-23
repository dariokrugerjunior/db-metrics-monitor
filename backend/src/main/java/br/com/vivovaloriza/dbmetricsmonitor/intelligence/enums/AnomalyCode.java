package br.com.vivovaloriza.dbmetricsmonitor.intelligence.enums;

public enum AnomalyCode {
    TOTAL_CONNECTIONS_SPIKE,
    ACTIVE_CONNECTIONS_SPIKE,
    IDLE_IN_TRANSACTION_SPIKE,
    BLOCKED_LOCK_SPIKE,
    RUNNING_QUERIES_SPIKE,
    LONG_RUNNING_QUERIES_SPIKE,
    CACHE_HIT_DROP,
    CPU_SPIKE,
    MEMORY_SPIKE
}
