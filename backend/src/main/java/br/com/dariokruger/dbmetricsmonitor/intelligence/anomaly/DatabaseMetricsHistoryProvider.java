package br.com.dariokruger.dbmetricsmonitor.intelligence.anomaly;

import br.com.dariokruger.dbmetricsmonitor.intelligence.dto.DatabaseHealthSnapshot;
import java.util.List;

public interface DatabaseMetricsHistoryProvider {

    List<HistoricalMetricsSnapshot> loadBaseline(DatabaseHealthSnapshot snapshot);
}
