package br.com.vivovaloriza.dbmetricsmonitor.service;

import br.com.vivovaloriza.dbmetricsmonitor.dto.HealthResponse;
import br.com.vivovaloriza.dbmetricsmonitor.repository.DatabaseMonitoringRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HealthService {

    private final DatabaseMonitoringRepository repository;

    public HealthResponse health() {
        boolean databaseUp = repository.isDatabaseUp();
        return new HealthResponse(
                databaseUp ? "UP" : "DEGRADED",
                databaseUp ? "UP" : "DOWN",
                Instant.now()
        );
    }
}
