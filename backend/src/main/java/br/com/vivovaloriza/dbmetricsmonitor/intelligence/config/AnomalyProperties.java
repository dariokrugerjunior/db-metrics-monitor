package br.com.vivovaloriza.dbmetricsmonitor.intelligence.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnomalyProperties {

    private int baselineWindowMinutes = 120;
    private int minimumSamples = 6;
    private double totalConnectionsPercentThreshold = 40.0d;
    private double connectionActivePercentThreshold = 50.0d;
    private double idleInTransactionPercentThreshold = 50.0d;
    private double blockedLocksPercentThreshold = 100.0d;
    private double runningQueriesPercentThreshold = 50.0d;
    private double longRunningQueriesPercentThreshold = 50.0d;
    private double cacheHitDropThreshold = 2.0d;
    private double cpuPercentThreshold = 30.0d;
    private double memoryPercentThreshold = 25.0d;
}
