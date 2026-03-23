package br.com.vivovaloriza.dbmetricsmonitor.service;

import br.com.vivovaloriza.dbmetricsmonitor.dto.AiAnalysisHistoryResponse;
import br.com.vivovaloriza.dbmetricsmonitor.repository.AiAnalysisHistoryRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiAnalysisHistoryService {

    private final AiAnalysisHistoryRepository repository;

    @Value("${spring.datasource.url}")
    private String dbUrlAdmin;

    public void save(String model, String userPrompt, String finalPrompt, String analysis, Instant createdAt) {
        repository.save(dbUrlAdmin, model, userPrompt, finalPrompt, analysis, createdAt);
    }

    public List<AiAnalysisHistoryResponse> getCurrentDatabaseHistory(Integer limit) {
        int safeLimit = limit == null ? 100 : Math.min(Math.max(limit, 1), 500);
        return repository.findByDbUrlAdmin(dbUrlAdmin, safeLimit);
    }
}
