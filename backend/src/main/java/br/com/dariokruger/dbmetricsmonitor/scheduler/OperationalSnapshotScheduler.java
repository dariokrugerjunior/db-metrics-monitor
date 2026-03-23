package br.com.dariokruger.dbmetricsmonitor.scheduler;

import br.com.dariokruger.dbmetricsmonitor.model.OperationalSnapshot;
import br.com.dariokruger.dbmetricsmonitor.service.DatabaseMonitoringService;
import br.com.dariokruger.dbmetricsmonitor.service.DashboardService;
import br.com.dariokruger.dbmetricsmonitor.service.HistoricalIncidentService;
import br.com.dariokruger.dbmetricsmonitor.service.OperationalSnapshotPublisher;
import br.com.dariokruger.dbmetricsmonitor.intelligence.anomaly.DatabaseMetricsHistoryRecorder;
import br.com.dariokruger.dbmetricsmonitor.intelligence.service.DatabaseHealthSnapshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OperationalSnapshotScheduler {

    private final DashboardService dashboardService;
    private final DatabaseMonitoringService databaseMonitoringService;
    private final HistoricalIncidentService historicalIncidentService;
    private final OperationalSnapshotPublisher publisher;
    private final DatabaseHealthSnapshotService databaseHealthSnapshotService;
    private final DatabaseMetricsHistoryRecorder databaseMetricsHistoryRecorder;

    @Scheduled(cron = "${app.scheduler.snapshot-cron}")
    public void collectSnapshot() {
        log.info("snapshot_collection_started");
        OperationalSnapshot snapshot = new OperationalSnapshot(java.time.Instant.now(), dashboardService.summary());
        historicalIncidentService.recordSnapshotIncidents(
                snapshot.summary(),
                databaseMonitoringService.getBlockedLocks(),
                snapshot.generatedAt()
        );
        databaseMetricsHistoryRecorder.record(databaseHealthSnapshotService.capture());
        publisher.publish(snapshot);
        log.info("snapshot_collection_finished generatedAt={}", snapshot.generatedAt());
    }
}
