package br.com.vivovaloriza.dbmetricsmonitor.service.prompt;

import br.com.vivovaloriza.dbmetricsmonitor.dto.DatabaseHealthSnapshot;
import br.com.vivovaloriza.dbmetricsmonitor.dto.HistoricalIncidentSummaryResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class HealthSignalAnalyzer {

    private static final BigDecimal CONNECTION_WARNING_THRESHOLD = BigDecimal.valueOf(70);
    private static final BigDecimal CONNECTION_CRITICAL_THRESHOLD = BigDecimal.valueOf(85);
    private static final BigDecimal CACHE_WARNING_THRESHOLD = BigDecimal.valueOf(97);
    private static final BigDecimal CACHE_HEALTHY_THRESHOLD = BigDecimal.valueOf(99);
    private static final BigDecimal SLOW_QUERY_THRESHOLD_MS = BigDecimal.valueOf(100);
    private static final int RECURRENT_INCIDENT_THRESHOLD = 5;

    public List<String> analyze(DatabaseHealthSnapshot snapshot, HistoricalIncidentSummaryResponse history) {
        List<String> signals = new ArrayList<>();

        if (snapshot.locks().blockedLocks() > 0) {
            signals.add("Ha locks bloqueados no momento.");
        }
        if (snapshot.locks().blockedSessions().stream().anyMatch(lock -> lock.blockedByPid() != null)) {
            signals.add("Ha sessoes bloqueadas aguardando liberacao de lock.");
        }
        if (snapshot.connections().idleInTransactionConnections() > 0) {
            signals.add("Ha sessoes idle in transaction.");
        }
        if (snapshot.connections().usagePercent().compareTo(CONNECTION_CRITICAL_THRESHOLD) > 0) {
            signals.add("Uso de conexoes elevado acima de 85%.");
        } else if (snapshot.connections().usagePercent().compareTo(CONNECTION_WARNING_THRESHOLD) > 0) {
            signals.add("Uso de conexoes acima de 70%.");
        }
        if (hasSlowQueries(snapshot)) {
            signals.add("Existem queries lentas relevantes.");
        }
        if (snapshot.cache().cacheHitPercent().compareTo(CACHE_WARNING_THRESHOLD) < 0) {
            signals.add("Cache hit ratio abaixo de 97%, indicando leitura excessiva em disco.");
        } else if (snapshot.cache().cacheHitPercent().compareTo(CACHE_HEALTHY_THRESHOLD) < 0) {
            signals.add("Cache hit ratio entre 97% e 99%, exige acompanhamento.");
        }
        if (history.totalIncidents() >= RECURRENT_INCIDENT_THRESHOLD) {
            signals.add("Ha recorrencia de incidentes nas ultimas 24h.");
        }
        if (signals.isEmpty()) {
            signals.add("Nao ha sinais relevantes no snapshot atual.");
        }

        return signals.stream()
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private boolean hasSlowQueries(DatabaseHealthSnapshot snapshot) {
        return snapshot.queries().topQueries().stream()
                .anyMatch(query -> query.meanExecTime() != null
                        && query.meanExecTime().compareTo(SLOW_QUERY_THRESHOLD_MS) >= 0);
    }
}
