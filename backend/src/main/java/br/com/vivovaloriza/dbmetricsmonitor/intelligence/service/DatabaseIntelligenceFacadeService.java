package br.com.vivovaloriza.dbmetricsmonitor.intelligence.service;

import br.com.vivovaloriza.dbmetricsmonitor.intelligence.alerts.DatabaseAlertEngine;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.anomaly.DatabaseAnomalyDetectionService;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.AnomalyDetectionResponse;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.DatabaseAlertResponse;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.DatabaseHealthScoreResponse;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.DatabaseIntelligenceOverviewResponse;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.DatabaseRecommendationResponse;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.recommendation.DatabaseRecommendationService;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.scoring.DatabaseHealthScoreService;
import org.springframework.stereotype.Service;

@Service
public class DatabaseIntelligenceFacadeService {

    private final DatabaseHealthSnapshotService snapshotService;
    private final DatabaseHealthScoreService scoreService;
    private final DatabaseAnomalyDetectionService anomalyDetectionService;
    private final DatabaseAlertEngine alertEngine;
    private final DatabaseRecommendationService recommendationService;

    public DatabaseIntelligenceFacadeService(
            DatabaseHealthSnapshotService snapshotService,
            DatabaseHealthScoreService scoreService,
            DatabaseAnomalyDetectionService anomalyDetectionService,
            DatabaseAlertEngine alertEngine,
            DatabaseRecommendationService recommendationService
    ) {
        this.snapshotService = snapshotService;
        this.scoreService = scoreService;
        this.anomalyDetectionService = anomalyDetectionService;
        this.alertEngine = alertEngine;
        this.recommendationService = recommendationService;
    }

    public DatabaseHealthScoreResponse score() {
        var snapshot = snapshotService.capture();
        return scoreService.calculate(snapshot);
    }

    public DatabaseAlertResponse alerts() {
        var snapshot = snapshotService.capture();
        AnomalyDetectionResponse anomalyResponse = anomalyDetectionService.detect(snapshot);
        return alertEngine.evaluate(snapshot, anomalyResponse.anomalies());
    }

    public AnomalyDetectionResponse anomalies() {
        return anomalyDetectionService.detect(snapshotService.capture());
    }

    public DatabaseRecommendationResponse recommendations() {
        var snapshot = snapshotService.capture();
        AnomalyDetectionResponse anomalyResponse = anomalyDetectionService.detect(snapshot);
        DatabaseAlertResponse alertResponse = alertEngine.evaluate(snapshot, anomalyResponse.anomalies());
        return recommendationService.generate(snapshot, alertResponse.alerts(), anomalyResponse.anomalies());
    }

    public DatabaseIntelligenceOverviewResponse overview() {
        var snapshot = snapshotService.capture();
        DatabaseHealthScoreResponse scoreResponse = scoreService.calculate(snapshot);
        AnomalyDetectionResponse anomalyResponse = anomalyDetectionService.detect(snapshot);
        DatabaseAlertResponse alertResponse = alertEngine.evaluate(snapshot, anomalyResponse.anomalies());
        DatabaseRecommendationResponse recommendationResponse = recommendationService.generate(
                snapshot,
                alertResponse.alerts(),
                anomalyResponse.anomalies()
        );
        return new DatabaseIntelligenceOverviewResponse(
                scoreResponse,
                alertResponse.alerts(),
                anomalyResponse.anomalies(),
                recommendationResponse.recommendations(),
                snapshot.collectedAt(),
                anomalyResponse.message()
        );
    }
}
