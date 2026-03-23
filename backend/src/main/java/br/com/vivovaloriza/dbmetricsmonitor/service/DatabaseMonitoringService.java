package br.com.vivovaloriza.dbmetricsmonitor.service;

import br.com.vivovaloriza.dbmetricsmonitor.config.AppProperties;
import br.com.vivovaloriza.dbmetricsmonitor.dto.CacheHitRatioResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.ConnectionSummaryResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.DatabaseSettingResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.GroupedConnectionResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.IdleSessionResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.LockInfoResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.QueryStatsResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.RunningQueryResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.TableAccessResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.TableSizeResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.TerminateSessionRequest;
import br.com.vivovaloriza.dbmetricsmonitor.dto.TerminateSessionResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.TopQueryResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.VacuumHealthResponse;
import br.com.vivovaloriza.dbmetricsmonitor.exception.BadRequestException;
import br.com.vivovaloriza.dbmetricsmonitor.exception.DatabaseOperationException;
import br.com.vivovaloriza.dbmetricsmonitor.exception.ResourceNotFoundException;
import br.com.vivovaloriza.dbmetricsmonitor.repository.DatabaseMonitoringRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseMonitoringService {

    private final DatabaseMonitoringRepository repository;
    private final AppProperties appProperties;
    @Value("${spring.application.name:db-metrics-monitor}")
    private String applicationName;

    public List<LockInfoResponse> getLocks() {
        return repository.findAllLocks();
    }

    public List<LockInfoResponse> getBlockingLocks() {
        return repository.findBlockingLocks();
    }

    public List<LockInfoResponse> getBlockedLocks() {
        return repository.findBlockedLocks();
    }

    public List<RunningQueryResponse> getRunningQueries(long minDurationSeconds) {
        validateMinDuration(minDurationSeconds);
        return repository.findRunningQueriesAboveSeconds(minDurationSeconds);
    }

    public ConnectionSummaryResponse getConnectionSummary() {
        Map<String, Object> counters = repository.findConnectionCounters();
        int total = ((Number) counters.get("total_connections")).intValue();
        int active = ((Number) counters.get("active_connections")).intValue();
        int idle = ((Number) counters.get("idle_connections")).intValue();
        int idleInTransaction = ((Number) counters.get("idle_in_transaction_connections")).intValue();
        int max = ((Number) counters.get("max_connections")).intValue();
        BigDecimal usagePercent = max == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(total * 100.0d / max).setScale(2, RoundingMode.HALF_UP);

        List<GroupedConnectionResponse> byUser = repository.findConnectionsByUser();
        List<GroupedConnectionResponse> byApplication = repository.findConnectionsByApplication();

        return new ConnectionSummaryResponse(total, active, idle, idleInTransaction, max, usagePercent, byUser, byApplication);
    }

    public List<GroupedConnectionResponse> getConnectionsByUser() {
        return repository.findConnectionsByUser();
    }

    public List<GroupedConnectionResponse> getConnectionsByApplication() {
        return repository.findConnectionsByApplication();
    }

    public QueryStatsResponse getTopQueries(Integer limit) {
        int safeLimit = normalizeLimit(limit);
        return loadQueryStats(() -> repository.findTopQueries(safeLimit));
    }

    public QueryStatsResponse getSlowQueries(Integer limit) {
        int safeLimit = normalizeLimit(limit);
        return loadQueryStats(() -> repository.findSlowQueries(safeLimit));
    }

    public QueryStatsResponse getQueriesByMeanTime(Integer limit) {
        int safeLimit = normalizeLimit(limit);
        return loadQueryStats(() -> repository.findQueriesByMeanTime(safeLimit));
    }

    public TerminateSessionResponse terminateSession(long pid, TerminateSessionRequest request) {
        validatePid(pid);
        log.info("terminate_session_attempt pid={} reason={} force={}", pid, sanitizeReason(request.reason()), request.force());

        if (!Boolean.TRUE.equals(request.force())) {
            throw new BadRequestException("O campo force deve ser true para confirmar a finalizacao.");
        }
        if (!repository.sessionExists(pid)) {
            throw new ResourceNotFoundException("PID informado nao foi encontrado no pg_stat_activity.");
        }

        long currentBackendPid = repository.getCurrentBackendPid();
        if (pid == currentBackendPid) {
            log.warn("terminate_session_blocked_self_backend pid={}", pid);
            throw new BadRequestException("Nao e permitido encerrar o backend atual da propria aplicacao.");
        }

        String targetApplicationName = repository.findSessionApplicationName(pid);
        if (applicationName.equalsIgnoreCase(targetApplicationName)) {
            log.warn("terminate_session_blocked_application_backend pid={} application_name={}", pid, targetApplicationName);
            throw new BadRequestException("Nao e permitido encerrar uma sessao identificada como pertencente a esta aplicacao.");
        }

        try {
            boolean terminated = repository.terminateSession(pid);
            String message = terminated
                    ? "Sessao finalizada com sucesso"
                    : "A sessao nao pode ser finalizada. Verifique permissoes ou estado atual.";
            log.info("terminate_session_result pid={} terminated={}", pid, terminated);
            return new TerminateSessionResponse(pid, terminated, message);
        } catch (DataAccessException ex) {
            log.error("terminate_session_error pid={} message={}", pid, ex.getMessage(), ex);
            throw new DatabaseOperationException("Erro ao tentar finalizar a sessao no PostgreSQL.", ex);
        }
    }

    public List<TableSizeResponse> getTopTableSizes(Integer limit) {
        return repository.findTopTableSizes(normalizeLimit(limit));
    }

    public List<TableAccessResponse> getTopTableAccesses(Integer limit) {
        return repository.findTopTableAccesses(normalizeLimit(limit));
    }

    public List<VacuumHealthResponse> getVacuumHealth(Integer limit) {
        return repository.findVacuumHealth(normalizeLimit(limit));
    }

    public CacheHitRatioResponse getCacheHitRatio() {
        return repository.findCacheHitRatio();
    }

    public List<IdleSessionResponse> getIdleInTransactionSessions(long minDurationSeconds) {
        validateMinDuration(minDurationSeconds);
        return repository.findIdleInTransactionSessions(minDurationSeconds);
    }

    public List<DatabaseSettingResponse> getDatabaseSettings() {
        return repository.findDatabaseSettings();
    }

    public int defaultRunningQueryThreshold() {
        return appProperties.getMonitoring().getRunningQueryDefaultMinSeconds();
    }

    private QueryStatsResponse loadQueryStats(QueryLoader queryLoader) {
        if (!repository.isPgStatStatementsAvailable()) {
            return new QueryStatsResponse(
                    false,
                    "A extensao pg_stat_statements nao esta habilitada. Ative com CREATE EXTENSION pg_stat_statements; e configure shared_preload_libraries.",
                    List.of()
            );
        }
        return new QueryStatsResponse(true, "Consulta executada com sucesso.", queryLoader.load());
    }

    private int normalizeLimit(Integer limit) {
        int resolvedLimit = limit == null ? appProperties.getMonitoring().getTopQueryDefaultLimit() : limit;
        if (resolvedLimit < 1 || resolvedLimit > 100) {
            throw new BadRequestException("O parametro limit deve estar entre 1 e 100.");
        }
        return resolvedLimit;
    }

    private void validatePid(long pid) {
        if (pid <= 0) {
            throw new BadRequestException("PID deve ser maior que zero.");
        }
    }

    private void validateMinDuration(long minDurationSeconds) {
        if (minDurationSeconds < 0) {
            throw new BadRequestException("minDurationSeconds deve ser maior ou igual a zero.");
        }
    }

    private String sanitizeReason(String reason) {
        return reason == null || reason.isBlank() ? "not_provided" : reason.replaceAll("[\\r\\n]+", " ").trim();
    }

    @FunctionalInterface
    private interface QueryLoader {
        List<TopQueryResponse> load();
    }
}
