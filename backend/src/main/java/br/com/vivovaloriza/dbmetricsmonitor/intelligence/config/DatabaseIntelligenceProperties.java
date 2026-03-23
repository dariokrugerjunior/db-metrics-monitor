package br.com.vivovaloriza.dbmetricsmonitor.intelligence.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "db.intelligence")
public class DatabaseIntelligenceProperties {

    private String environment = "default";
    private final ScoreProperties score = new ScoreProperties();
    private final AlertProperties alerts = new AlertProperties();
    private final AnomalyProperties anomaly = new AnomalyProperties();
    private final RecommendationProperties recommendation = new RecommendationProperties();

    @Getter
    @Setter
    public static class RecommendationProperties {
        private int lockIncidentRecurrenceThreshold = 3;
        private double topQueryTotalExecTimeThreshold = 10000.0d;
    }
}
