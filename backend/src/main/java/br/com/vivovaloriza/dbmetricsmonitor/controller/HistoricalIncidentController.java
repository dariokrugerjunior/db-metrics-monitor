package br.com.vivovaloriza.dbmetricsmonitor.controller;

import br.com.vivovaloriza.dbmetricsmonitor.dto.HistoricalIncidentPageResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.HistoricalIncidentResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.HistoricalIncidentSummaryResponse;
import br.com.vivovaloriza.dbmetricsmonitor.service.HistoricalIncidentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/history")
@RequiredArgsConstructor
@Tag(name = "History", description = "Historico de incidentes operacionais")
public class HistoricalIncidentController {

    private final HistoricalIncidentService historicalIncidentService;

    @GetMapping("/incidents")
    @Operation(summary = "Lista incidentes historicos persistidos localmente")
    public List<HistoricalIncidentResponse> incidents(@RequestParam(required = false) Integer limit) {
        return historicalIncidentService.getRecentIncidents(limit);
    }

    @GetMapping("/incidents/page")
    @Operation(summary = "Lista incidentes historicos com paginacao")
    public HistoricalIncidentPageResponse incidentsPage(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return historicalIncidentService.getIncidentsPage(page, size);
    }

    @GetMapping("/summary")
    @Operation(summary = "Resumo agregado dos incidentes historicos")
    public HistoricalIncidentSummaryResponse summary() {
        return historicalIncidentService.getSummary();
    }

    @DeleteMapping("/incidents")
    @Operation(summary = "Remove todo o historico de incidentes operacionais")
    public void clearHistory() {
        historicalIncidentService.clearHistory();
    }
}
