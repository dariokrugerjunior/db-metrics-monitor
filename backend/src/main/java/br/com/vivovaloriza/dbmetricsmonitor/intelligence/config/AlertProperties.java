package br.com.vivovaloriza.dbmetricsmonitor.intelligence.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlertProperties {

    private double connectionWarningPercent = 70.0d;
    private double connectionCriticalPercent = 85.0d;
    private double cpuWarningPercent = 80.0d;
    private double cpuCriticalPercent = 90.0d;
    private double memoryWarningPercent = 80.0d;
    private double memoryCriticalPercent = 90.0d;
    private double cacheWarningPercent = 99.0d;
    private double cacheCriticalPercent = 97.0d;
    private long longQueryWarningSeconds = 60;
    private long longQueryCriticalSeconds = 300;
    private int idleInTransactionWarningCount = 1;
    private int recurringIncidentThreshold = 5;
    private int recurringLockIncidentThreshold = 3;
}
