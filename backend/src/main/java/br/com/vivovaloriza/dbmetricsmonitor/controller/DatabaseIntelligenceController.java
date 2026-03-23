package br.com.vivovaloriza.dbmetricsmonitor.controller;

import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.AnomalyDetectionResponse;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.DatabaseAlertResponse;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.DatabaseHealthScoreResponse;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.DatabaseIntelligenceOverviewResponse;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.DatabaseRecommendationResponse;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.service.DatabaseIntelligenceFacadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/db/intelligence")
@RequiredArgsConstructor
@Tag(name = "Database Intelligence", description = "Motor deterministico de score, alertas, anomalias e recomendacoes")
public class DatabaseIntelligenceController {

    private final DatabaseIntelligenceFacadeService service;

    @GetMapping("/overview")
    @Operation(summary = "Retorna a visao consolidada de inteligencia operacional")
    public DatabaseIntelligenceOverviewResponse overview() {
        return service.overview();
    }

    @GetMapping("/score")
    @Operation(summary = "Retorna o score de saude do banco")
    public DatabaseHealthScoreResponse score() {
        return service.score();
    }

    @GetMapping("/alerts")
    @Operation(summary = "Retorna os alertas ativos do ambiente")
    public DatabaseAlertResponse alerts() {
        return service.alerts();
    }

    @GetMapping("/anomalies")
    @Operation(summary = "Retorna as anomalias detectadas com base em baseline historica")
    public AnomalyDetectionResponse anomalies() {
        return service.anomalies();
    }

    @GetMapping("/recommendations")
    @Operation(summary = "Retorna recomendacoes acionaveis baseadas em correlacao de sinais")
    public DatabaseRecommendationResponse recommendations() {
        return service.recommendations();
    }
}
