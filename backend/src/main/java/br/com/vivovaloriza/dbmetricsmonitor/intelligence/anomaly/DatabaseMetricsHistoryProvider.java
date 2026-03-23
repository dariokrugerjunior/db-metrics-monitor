package br.com.vivovaloriza.dbmetricsmonitor.intelligence.anomaly;

import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.DatabaseHealthSnapshot;
import java.util.List;

public interface DatabaseMetricsHistoryProvider {

    List<HistoricalMetricsSnapshot> loadBaseline(DatabaseHealthSnapshot snapshot);
}
