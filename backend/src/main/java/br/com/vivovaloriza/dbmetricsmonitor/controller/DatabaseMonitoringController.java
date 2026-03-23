package br.com.vivovaloriza.dbmetricsmonitor.controller;

import br.com.vivovaloriza.dbmetricsmonitor.dto.CacheHitRatioResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.ConnectionSummaryResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.GroupedConnectionResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.IdleSessionResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.LockInfoResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.QueryStatsResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.RunningQueryResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.TableAccessResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.TableSizeResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.TerminateSessionRequest;
import br.com.vivovaloriza.dbmetricsmonitor.dto.TerminateSessionResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.VacuumHealthResponse;
import br.com.vivovaloriza.dbmetricsmonitor.service.DatabaseMonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/db")
@RequiredArgsConstructor
@Validated
@Tag(name = "Database Monitoring", description = "Endpoints operacionais para PostgreSQL")
public class DatabaseMonitoringController {

    private final DatabaseMonitoringService service;

    @GetMapping("/locks")
    @Operation(summary = "Lista locks ativos no PostgreSQL")
    public List<LockInfoResponse> getLocks() {
        return service.getLocks();
    }

    @GetMapping("/locks/blocking")
    @Operation(summary = "Lista sessoes que estao bloqueando outras")
    public List<LockInfoResponse> getBlockingLocks() {
        return service.getBlockingLocks();
    }

    @GetMapping("/locks/blocked")
    @Operation(summary = "Lista sessoes bloqueadas")
    public List<LockInfoResponse> getBlockedLocks() {
        return service.getBlockedLocks();
    }

    @PostMapping("/sessions/{pid}/terminate")
    @Operation(summary = "Finaliza uma sessao do PostgreSQL com seguranca")
    public TerminateSessionResponse terminateSession(
            @PathVariable long pid,
            @Valid @RequestBody TerminateSessionRequest request
    ) {
        return service.terminateSession(pid, request);
    }

    @GetMapping("/queries/top")
    @Operation(summary = "Lista queries mais executadas via pg_stat_statements")
    public QueryStatsResponse getTopQueries(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer limit
    ) {
        return service.getTopQueries(limit);
    }

    @GetMapping("/queries/slow")
    @Operation(summary = "Lista queries mais lentas por tempo total")
    public QueryStatsResponse getSlowQueries(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer limit
    ) {
        return service.getSlowQueries(limit);
    }

    @GetMapping("/queries/by-mean-time")
    @Operation(summary = "Lista queries ordenadas por tempo medio")
    public QueryStatsResponse getQueriesByMeanTime(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer limit
    ) {
        return service.getQueriesByMeanTime(limit);
    }

    @GetMapping("/queries/running")
    @Operation(summary = "Lista queries em execucao acima de um tempo minimo")
    public List<RunningQueryResponse> getRunningQueries(
            @RequestParam(defaultValue = "30")
            @Parameter(description = "Duracao minima em segundos")
            @Min(0) long minDurationSeconds
    ) {
        return service.getRunningQueries(minDurationSeconds);
    }

    @GetMapping("/connections/summary")
    @Operation(summary = "Resumo geral das conexoes do banco")
    public ConnectionSummaryResponse getConnectionSummary() {
        return service.getConnectionSummary();
    }

    @GetMapping("/connections/by-user")
    @Operation(summary = "Agrupamento de conexoes por usuario")
    public List<GroupedConnectionResponse> getConnectionsByUser() {
        return service.getConnectionsByUser();
    }

    @GetMapping("/connections/by-application")
    @Operation(summary = "Agrupamento de conexoes por application_name")
    public List<GroupedConnectionResponse> getConnectionsByApplication() {
        return service.getConnectionsByApplication();
    }

    @GetMapping("/tables/top-size")
    @Operation(summary = "Lista tabelas e indices por tamanho")
    public List<TableSizeResponse> getTopTableSizes(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer limit
    ) {
        return service.getTopTableSizes(limit);
    }

    @GetMapping("/tables/top-access")
    @Operation(summary = "Lista tabelas mais acessadas")
    public List<TableAccessResponse> getTopTableAccess(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer limit
    ) {
        return service.getTopTableAccesses(limit);
    }

    @GetMapping("/vacuum/health")
    @Operation(summary = "Exibe saude de vacuum e dead tuples")
    public List<VacuumHealthResponse> getVacuumHealth(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer limit
    ) {
        return service.getVacuumHealth(limit);
    }

    @GetMapping("/cache/hit-ratio")
    @Operation(summary = "Exibe cache hit ratio do banco")
    public CacheHitRatioResponse getCacheHitRatio() {
        return service.getCacheHitRatio();
    }

    @GetMapping("/sessions/idle-in-transaction")
    @Operation(summary = "Lista sessoes idle in transaction acima do tempo informado")
    public List<IdleSessionResponse> getIdleInTransactionSessions(
            @RequestParam(defaultValue = "60") @Min(0) long minDurationSeconds
    ) {
        return service.getIdleInTransactionSessions(minDurationSeconds);
    }
}
