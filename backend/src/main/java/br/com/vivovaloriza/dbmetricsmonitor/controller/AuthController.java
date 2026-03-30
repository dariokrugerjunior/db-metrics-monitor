package br.com.vivovaloriza.dbmetricsmonitor.controller;

import br.com.vivovaloriza.dbmetricsmonitor.dto.DatabaseAuthRequest;
import br.com.vivovaloriza.dbmetricsmonitor.dto.DatabaseAuthResponse;
import br.com.vivovaloriza.dbmetricsmonitor.service.RuntimeConfigurationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Autenticacao por conexao com banco de dados")
public class AuthController {

    private final RuntimeConfigurationService runtimeConfigurationService;

    @PostMapping("/connect")
    @Operation(summary = "Testa e autentica via conexao real com o banco de dados")
    public DatabaseAuthResponse connect(@Valid @RequestBody DatabaseAuthRequest request) {
        return runtimeConfigurationService.connectAndSaveDatabase(request);
    }
}
