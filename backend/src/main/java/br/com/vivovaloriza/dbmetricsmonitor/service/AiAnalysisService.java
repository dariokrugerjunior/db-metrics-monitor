package br.com.vivovaloriza.dbmetricsmonitor.service;

import br.com.vivovaloriza.dbmetricsmonitor.config.AppProperties;
import br.com.vivovaloriza.dbmetricsmonitor.dto.AiAnalysisRequest;
import br.com.vivovaloriza.dbmetricsmonitor.dto.AiAnalysisResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.CacheHitRatioResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.ConnectionSummaryResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.DatabaseHealthSnapshot;
import br.com.vivovaloriza.dbmetricsmonitor.dto.DatabaseSettingResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.DashboardSummaryResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.HistoricalIncidentSummaryResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.LockInfoResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.TopQueryResponse;
import br.com.vivovaloriza.dbmetricsmonitor.exception.BadRequestException;
import br.com.vivovaloriza.dbmetricsmonitor.exception.ExternalIntegrationException;
import br.com.vivovaloriza.dbmetricsmonitor.service.prompt.DatabaseHealthPromptBuilder;
import br.com.vivovaloriza.dbmetricsmonitor.service.prompt.PromptFormattingUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiAnalysisService {

    private static final ConcurrentHashMap<String, ReentrantLock> ANALYSIS_LOCKS = new ConcurrentHashMap<>();
    private static final Set<String> IMPORTANT_SETTINGS = Set.of(
            "max_connections",
            "shared_buffers",
            "work_mem",
            "maintenance_work_mem",
            "effective_cache_size",
            "statement_timeout",
            "lock_timeout",
            "idle_in_transaction_session_timeout"
    );

    private final DashboardService dashboardService;
    private final DatabaseMonitoringService databaseMonitoringService;
    private final HistoricalIncidentService historicalIncidentService;
    private final AiAnalysisHistoryService aiAnalysisHistoryService;
    private final AppProperties appProperties;
    private final DatabaseHealthPromptBuilder databaseHealthPromptBuilder;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();

    @Value("${spring.datasource.url}")
    private String dbUrlAdmin;

    public AiAnalysisResponse analyze(AiAnalysisRequest request) {
        if (!appProperties.getAi().isEnabled()) {
            throw new BadRequestException("A analise IA esta desabilitada nesta aplicacao.");
        }
        if (appProperties.getAi().getApiKey() == null || appProperties.getAi().getApiKey().isBlank()) {
            throw new BadRequestException("APP_OPENAI_API_KEY nao configurada no backend.");
        }

        ReentrantLock lock = ANALYSIS_LOCKS.computeIfAbsent(dbUrlAdmin, ignored -> new ReentrantLock(true));
        lock.lock();
        try {
            DashboardSummaryResponse dashboard = dashboardService.summary();
            List<LockInfoResponse> blockedLocks = databaseMonitoringService.getBlockedLocks();
            HistoricalIncidentSummaryResponse history = historicalIncidentService.getSummaryLast24Hours();
            CacheHitRatioResponse cacheHitRatio = databaseMonitoringService.getCacheHitRatio();
            String userPrompt = request == null || request.prompt() == null ? "" : request.prompt().trim();
            DatabaseHealthSnapshot snapshot = buildSnapshot(dashboard, blockedLocks, cacheHitRatio);
            String context = databaseHealthPromptBuilder.build(snapshot, history, userPrompt);
            String analysis = callOpenAi(context);
            Instant generatedAt = Instant.now();
            aiAnalysisHistoryService.save(
                    appProperties.getAi().getModel(),
                    userPrompt,
                    context,
                    analysis,
                    generatedAt
            );

            return new AiAnalysisResponse(
                    appProperties.getAi().getModel(),
                    userPrompt,
                    analysis,
                    generatedAt
            );
        } finally {
            lock.unlock();
        }
    }

    private DatabaseHealthSnapshot buildSnapshot(
            DashboardSummaryResponse dashboard,
            List<LockInfoResponse> blockedLocks,
            CacheHitRatioResponse cacheHitRatio
    ) {
        ConnectionSummaryResponse connections = dashboard.database().connections();

        return new DatabaseHealthSnapshot(
                new DatabaseHealthSnapshot.ConnectionsSnapshot(
                        connections.totalConnections(),
                        connections.activeConnections(),
                        connections.idleConnections(),
                        connections.idleInTransactionConnections(),
                        connections.maxConnections(),
                        scale(connections.usagePercent()),
                        connections.byUser().stream()
                                .map(group -> new DatabaseHealthSnapshot.ConnectionGroupSnapshot(group.name(), group.connections()))
                                .toList(),
                        connections.byApplication().stream()
                                .map(group -> new DatabaseHealthSnapshot.ConnectionGroupSnapshot(group.name(), group.connections()))
                                .toList()
                ),
                new DatabaseHealthSnapshot.LocksSnapshot(
                        dashboard.database().locks().total(),
                        dashboard.database().locks().blocked(),
                        dashboard.database().locks().blocking(),
                        blockedLocks.stream()
                                .sorted(Comparator.comparing(LockInfoResponse::queryDuration, Comparator.nullsLast(Comparator.reverseOrder())))
                                .limit(5)
                                .map(lock -> new DatabaseHealthSnapshot.LockDetailSnapshot(
                                        lock.pid(),
                                        lock.relation(),
                                        lock.state(),
                                        lock.applicationName(),
                                        lock.waitEventType(),
                                        lock.waitEvent(),
                                        PromptFormattingUtils.formatDuration(lock.queryDuration()),
                                        lock.blockedByPid(),
                                        PromptFormattingUtils.truncateQuery(lock.query())
                                ))
                                .toList()
                ),
                new DatabaseHealthSnapshot.QueriesSnapshot(
                        dashboard.database().runningQueries().total(),
                        dashboard.database().runningQueries().queries().stream()
                                .sorted(Comparator.comparing(query -> query.duration() == null ? Duration.ZERO : query.duration(), Comparator.reverseOrder()))
                                .limit(5)
                                .map(query -> new DatabaseHealthSnapshot.RunningQuerySnapshot(
                                        query.pid(),
                                        PromptFormattingUtils.formatDuration(query.duration()),
                                        query.state(),
                                        query.waitEvent(),
                                        PromptFormattingUtils.truncateQuery(query.query())
                                ))
                                .toList(),
                        dashboard.database().topQueries().stream()
                                .sorted(Comparator.comparing(TopQueryResponse::meanExecTime, Comparator.nullsLast(Comparator.reverseOrder())))
                                .limit(5)
                                .map(query -> new DatabaseHealthSnapshot.QueryDetailSnapshot(
                                        scale(query.meanExecTime()),
                                        scale(query.totalExecTime()),
                                        query.calls(),
                                        PromptFormattingUtils.truncateQuery(query.query())
                                ))
                                .toList(),
                        dashboard.database().settings().stream()
                                .filter(setting -> IMPORTANT_SETTINGS.contains(setting.name()))
                                .sorted(Comparator.comparing(DatabaseSettingResponse::name))
                                .map(setting -> new DatabaseHealthSnapshot.SettingSnapshot(setting.name(), formatSettingValue(setting)))
                                .toList()
                ),
                new DatabaseHealthSnapshot.CacheSnapshot(
                        scale(cacheHitRatio.cacheHitRatioPercent()),
                        classifyCache(cacheHitRatio.cacheHitRatioPercent()),
                        cacheHitRatio.heapBlksRead(),
                        cacheHitRatio.heapBlksHit()
                ),
                new DatabaseHealthSnapshot.SystemSnapshot(
                        dashboard.database().status(),
                        PromptFormattingUtils.round(dashboard.application().cpu().processCpuUsage()).doubleValue(),
                        calculateMemoryPercent(dashboard),
                        dashboard.application().threads().liveThreads(),
                        dashboard.application().uptimeSeconds()
                ),
                List.of(),
                dashboard.generatedAt()
        );
    }

    private String callOpenAi(String context) {
        try {
            String body = objectMapper.createObjectNode()
                    .put("model", appProperties.getAi().getModel())
                    .put("max_output_tokens", appProperties.getAi().getMaxOutputTokens())
                    .set("input", objectMapper.createArrayNode()
                            .add(objectMapper.createObjectNode()
                                    .put("role", "user")
                                    .set("content", objectMapper.createArrayNode()
                                            .add(objectMapper.createObjectNode()
                                                    .put("type", "input_text")
                                                    .put("text", context)))))
                    .toString();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(appProperties.getAi().getBaseUrl() + "/responses"))
                    .header("Authorization", "Bearer " + appProperties.getAi().getApiKey())
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ExternalIntegrationException("Falha ao consultar a OpenAI. Status HTTP " + response.statusCode() + ".");
            }

            JsonNode root = objectMapper.readTree(response.body());
            String outputText = root.path("output_text").asText("");
            if (!outputText.isBlank()) {
                return outputText.trim();
            }

            JsonNode output = root.path("output");
            for (JsonNode item : output) {
                for (JsonNode content : item.path("content")) {
                    String text = content.path("text").asText("");
                    if (!text.isBlank()) {
                        return text.trim();
                    }
                }
            }

            throw new ExternalIntegrationException("A OpenAI respondeu sem conteudo analisavel.");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ExternalIntegrationException("Erro ao comunicar com a OpenAI.", ex);
        } catch (IOException ex) {
            throw new ExternalIntegrationException("Erro ao comunicar com a OpenAI.", ex);
        }
    }

    private BigDecimal scale(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String classifyCache(BigDecimal cacheHitPercent) {
        BigDecimal safeValue = cacheHitPercent == null ? BigDecimal.ZERO : cacheHitPercent;
        if (safeValue.compareTo(BigDecimal.valueOf(99)) >= 0) {
            return "saudavel";
        }
        if (safeValue.compareTo(BigDecimal.valueOf(97)) >= 0) {
            return "atencao";
        }
        return "critico";
    }

    private String formatSettingValue(DatabaseSettingResponse setting) {
        return PromptFormattingUtils.formatSettingValue(setting.setting(), setting.unit());
    }

    private double calculateMemoryPercent(DashboardSummaryResponse dashboard) {
        long totalMemory = dashboard.application().memory().jvmTotalMemoryBytes();
        if (totalMemory <= 0) {
            return 0.0d;
        }
        return BigDecimal.valueOf(dashboard.application().memory().heapUsedBytes() * 100.0d / totalMemory)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
