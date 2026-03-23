package br.com.vivovaloriza.dbmetricsmonitor.intelligence.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScoreProperties {

    private int healthyMinScore = 80;
    private int warningMinScore = 50;
    private int recurringIncidentThreshold = 5;
    private int recurringLockIncidentThreshold = 3;
    private long longQueryWarningSeconds = 10;
    private long longQueryCriticalSeconds = 60;
    private final Penalties penalties = new Penalties();

    @Getter
    @Setter
    public static class Penalties {
        private int blockedLocks = 25;
        private int blockingLocks = 15;
        private int idleInTransaction = 10;
        private int idleInTransactionCritical = 20;
        private int highConnectionUsage = 10;
        private int criticalConnectionUsage = 20;
        private int longRunningQuery = 10;
        private int criticalLongRunningQuery = 20;
        private int lowCacheHit = 5;
        private int criticalCacheHit = 15;
        private int highCpu = 10;
        private int criticalCpu = 20;
        private int highMemory = 10;
        private int recurringIncidents = 10;
        private int recurringLockIncidents = 10;
    }
}
