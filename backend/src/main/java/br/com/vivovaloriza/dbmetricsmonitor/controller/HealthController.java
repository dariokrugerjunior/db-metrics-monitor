package br.com.vivovaloriza.dbmetricsmonitor.controller;

import br.com.vivovaloriza.dbmetricsmonitor.dto.HealthResponse;
import br.com.vivovaloriza.dbmetricsmonitor.service.HealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
@Tag(name = "Health", description = "Healthcheck da aplicacao e conectividade com o banco")
public class HealthController {

    private final HealthService healthService;

    @GetMapping
    @Operation(summary = "Retorna o status da aplicacao e do banco")
    public HealthResponse health() {
        return healthService.health();
    }
}
