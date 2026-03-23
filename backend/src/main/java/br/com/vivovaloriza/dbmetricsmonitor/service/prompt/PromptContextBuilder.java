package br.com.vivovaloriza.dbmetricsmonitor.service.prompt;

import br.com.vivovaloriza.dbmetricsmonitor.dto.DatabaseHealthSnapshot;
import br.com.vivovaloriza.dbmetricsmonitor.dto.HistoricalIncidentSummaryResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PromptContextBuilder {

    private final ObjectMapper objectMapper;

    public PromptContextBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String buildSignalsBlock(DatabaseHealthSnapshot snapshot) {
        return PromptFormattingUtils.bullets(snapshot.alerts());
    }

    public String buildDatabaseContext(DatabaseHealthSnapshot snapshot) {
        List<String> lines = new ArrayList<>();
        lines.add("Resumo de conexoes:");
        lines.add("  - total=" + snapshot.connections().totalConnections()
                + ", active=" + snapshot.connections().activeConnections()
                + ", idle=" + snapshot.connections().idleConnections()
                + ", idle_in_transaction=" + snapshot.connections().idleInTransactionConnections()
                + ", max=" + snapshot.connections().maxConnections()
                + ", usage_percent=" + PromptFormattingUtils.safe(snapshot.connections().usagePercent()));
        lines.add("Resumo de locks:");
        lines.add("  - total=" + snapshot.locks().totalLocks()
                + ", blocked=" + snapshot.locks().blockedLocks()
                + ", blocking=" + snapshot.locks().blockingLocks());
        lines.add("Locks bloqueados atuais:");
        lines.addAll(toNestedList(snapshot.locks().blockedSessions().stream()
                .map(lock -> "pid=" + lock.pid()
                        + ", relation=" + PromptFormattingUtils.safe(lock.relation())
                        + ", state=" + PromptFormattingUtils.safe(lock.state())
                        + ", duration=" + PromptFormattingUtils.safe(lock.duration())
                        + ", blocked_by_pid=" + PromptFormattingUtils.safe(lock.blockedByPid())
                        + " :: " + PromptFormattingUtils.safe(lock.query()))
                .toList()));
        lines.add("Queries em execucao:");
        lines.add("  - total=" + snapshot.queries().runningQueries());
        lines.addAll(toNestedList(snapshot.queries().runningSessions().stream()
                .map(query -> "pid=" + query.pid()
                        + ", duration=" + PromptFormattingUtils.safe(query.duration())
                        + ", state=" + PromptFormattingUtils.safe(query.state())
                        + ", wait_event=" + PromptFormattingUtils.safe(query.waitEvent())
                        + " :: " + PromptFormattingUtils.safe(query.query()))
                .toList()));
        lines.add("Top queries:");
        lines.addAll(toNestedList(snapshot.queries().topQueries().stream()
                .map(query -> "meanExecTime=" + PromptFormattingUtils.safe(query.meanExecTime()) + "ms"
                        + ", totalExecTime=" + PromptFormattingUtils.safe(query.totalExecTime()) + "ms"
                        + ", calls=" + PromptFormattingUtils.safe(query.calls())
                        + " :: " + PromptFormattingUtils.safe(query.query()))
                .toList()));
        lines.add("Settings relevantes:");
        lines.addAll(toNestedList(snapshot.queries().settings().stream()
                .map(setting -> setting.name() + "=" + setting.value())
                .toList()));
        lines.add("Cache:");
        lines.add("  - cacheHitPercent=" + PromptFormattingUtils.safe(snapshot.cache().cacheHitPercent())
                + ", classificacao=" + snapshot.cache().classification()
                + ", heapBlksRead=" + snapshot.cache().heapBlksRead()
                + ", heapBlksHit=" + snapshot.cache().heapBlksHit());
        lines.add("Sistema:");
        lines.add("  - database_status=" + snapshot.system().databaseStatus()
                + ", cpu_percent=" + snapshot.system().cpuPercent()
                + ", memory_percent=" + snapshot.system().memoryPercent()
                + ", live_threads=" + snapshot.system().liveThreads()
                + ", uptime_seconds=" + snapshot.system().uptimeSeconds());
        lines.add("Timestamp do snapshot:");
        lines.add("  - " + snapshot.timestamp());

        return String.join("\n", prefixTopLevel(lines));
    }

    public String buildHistoricalContext(HistoricalIncidentSummaryResponse history) {
        return PromptFormattingUtils.bullets(List.of(
                "Historico de incidentes nas ultimas 24h: total=" + history.totalIncidents()
                        + ", cpu=" + history.cpuIncidents()
                        + ", memory=" + history.memoryIncidents()
                        + ", locks=" + history.lockIncidents()
        ));
    }

    public String buildUserContext(String userPrompt) {
        if (userPrompt == null || userPrompt.isBlank()) {
            return PromptFormattingUtils.bullets(List.of("Nenhuma pergunta adicional do usuario."));
        }
        return PromptFormattingUtils.bullets(List.of(PromptFormattingUtils.compact(userPrompt)));
    }

    public String buildStructuredSnapshot(DatabaseHealthSnapshot snapshot) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(snapshot);
        } catch (JsonProcessingException ex) {
            return "{\"error\":\"snapshot_json_unavailable\"}";
        }
    }

    private List<String> toNestedList(List<String> items) {
        if (items == null || items.isEmpty()) {
            return List.of("  - Nenhum dado relevante.");
        }
        return items.stream()
                .map(item -> "  - " + item)
                .toList();
    }

    private List<String> prefixTopLevel(List<String> items) {
        return items.stream()
                .map(item -> item.startsWith("  - ") ? item : "- " + item)
                .toList();
    }
}
