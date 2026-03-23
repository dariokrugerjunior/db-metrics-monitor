package br.com.vivovaloriza.dbmetricsmonitor.controller;

import br.com.vivovaloriza.dbmetricsmonitor.dto.HistoricalIncidentResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.HistoricalIncidentSummaryResponse;
import br.com.vivovaloriza.dbmetricsmonitor.service.HistoricalIncidentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/history")
@RequiredArgsConstructor
@Tag(name = "History", description = "Histórico de incidentes operacionais")
public class HistoricalIncidentController {

    private final HistoricalIncidentService historicalIncidentService;

    @GetMapping("/incidents")
    @Operation(summary = "Lista incidentes históricos persistidos localmente")
    public List<HistoricalIncidentResponse> incidents(@RequestParam(required = false) Integer limit) {
        return historicalIncidentService.getRecentIncidents(limit);
    }

    @GetMapping("/summary")
    @Operation(summary = "Resumo agregado dos incidentes históricos")
    public HistoricalIncidentSummaryResponse summary() {
        return historicalIncidentService.getSummary();
    }
}
