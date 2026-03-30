package br.com.vivovaloriza.dbmetricsmonitor.controller;

import br.com.vivovaloriza.dbmetricsmonitor.dto.AppConfigurationResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.AppConfigurationUpdateRequest;
import br.com.vivovaloriza.dbmetricsmonitor.dto.DatabaseConnectionTestRequest;
import br.com.vivovaloriza.dbmetricsmonitor.dto.DatabaseConnectionTestResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.OpenAiConnectionTestRequest;
import br.com.vivovaloriza.dbmetricsmonitor.dto.OpenAiConnectionTestResponse;
import br.com.vivovaloriza.dbmetricsmonitor.service.RuntimeConfigurationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/configuration")
@RequiredArgsConstructor
@Tag(name = "Configuration", description = "Configuracoes persistidas da aplicacao")
public class ConfigurationController {

    private final RuntimeConfigurationService runtimeConfigurationService;

    @GetMapping
    @Operation(summary = "Consulta a configuracao persistida e o estado de aplicacao em runtime")
    public AppConfigurationResponse getConfiguration() {
        return runtimeConfigurationService.getConfiguration();
    }

    @PutMapping
    @Operation(summary = "Salva a configuracao da aplicacao no backend")
    public AppConfigurationResponse updateConfiguration(@Valid @RequestBody AppConfigurationUpdateRequest request) {
        return runtimeConfigurationService.updateConfiguration(request);
    }

    @PostMapping("/database/test")
    @Operation(summary = "Testa uma conexao PostgreSQL sem alterar o datasource atual")
    public DatabaseConnectionTestResponse testDatabaseConnection(@Valid @RequestBody DatabaseConnectionTestRequest request) {
        return runtimeConfigurationService.testConnection(request);
    }

    @PostMapping("/openai/test")
    @Operation(summary = "Testa a autenticacao da OpenAI sem alterar a configuracao atual")
    public OpenAiConnectionTestResponse testOpenAiConnection(@Valid @RequestBody OpenAiConnectionTestRequest request) {
        return runtimeConfigurationService.testOpenAiConnection(request);
    }
}
