package br.com.vivovaloriza.dbmetricsmonitor.controller;

import br.com.vivovaloriza.dbmetricsmonitor.dto.AiAnalysisHistoryResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.AiAnalysisRequest;
import br.com.vivovaloriza.dbmetricsmonitor.dto.AiAnalysisResponse;
import br.com.vivovaloriza.dbmetricsmonitor.service.AiAnalysisHistoryService;
import br.com.vivovaloriza.dbmetricsmonitor.service.AiAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "AI Analysis", description = "Análise assistida por IA com base no estado atual do banco")
public class AiAnalysisController {

    private final AiAnalysisService aiAnalysisService;
    private final AiAnalysisHistoryService aiAnalysisHistoryService;

    @PostMapping("/analysis")
    @Operation(summary = "Envia o contexto operacional do banco para análise IA")
    public AiAnalysisResponse analyze(@RequestBody(required = false) AiAnalysisRequest request) {
        return aiAnalysisService.analyze(request == null ? new AiAnalysisRequest("") : request);
    }

    @GetMapping("/history")
    @Operation(summary = "Lista histórico de análises IA filtrado pelo DB_URL_ADMIN atual")
    public List<AiAnalysisHistoryResponse> history(@RequestParam(required = false) Integer limit) {
        return aiAnalysisHistoryService.getCurrentDatabaseHistory(limit);
    }
}
