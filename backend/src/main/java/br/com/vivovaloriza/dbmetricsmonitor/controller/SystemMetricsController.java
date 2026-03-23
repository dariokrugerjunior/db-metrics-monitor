package br.com.vivovaloriza.dbmetricsmonitor.controller;

import br.com.vivovaloriza.dbmetricsmonitor.dto.SystemMetricsResponse;
import br.com.vivovaloriza.dbmetricsmonitor.service.SystemMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
@Tag(name = "System Metrics", description = "Metricas da JVM e do processo")
public class SystemMetricsController {

    private final SystemMetricsService systemMetricsService;

    @GetMapping("/metrics")
    @Operation(summary = "Retorna metricas de CPU, memoria, uptime e threads")
    public SystemMetricsResponse metrics() {
        return systemMetricsService.collect();
    }
}
