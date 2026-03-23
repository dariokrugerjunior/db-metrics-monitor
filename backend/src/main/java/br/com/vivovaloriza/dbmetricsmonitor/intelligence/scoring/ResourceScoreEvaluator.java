package br.com.vivovaloriza.dbmetricsmonitor.intelligence.scoring;

import br.com.vivovaloriza.dbmetricsmonitor.intelligence.config.DatabaseIntelligenceProperties;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.DatabaseHealthSnapshot;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.ScoreBreakdownResponse;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.ScorePenaltyResponse;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.enums.PenaltyCode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(50)
public class ResourceScoreEvaluator extends AbstractScoreCategoryEvaluator implements ScoreCategoryEvaluator {

    private final DatabaseIntelligenceProperties properties;

    public ResourceScoreEvaluator(DatabaseIntelligenceProperties properties) {
        this.properties = properties;
    }

    @Override
    public ScoreBreakdownResponse evaluate(DatabaseHealthSnapshot snapshot) {
        List<ScorePenaltyResponse> penalties = new ArrayList<>();
        double cpuPercent = snapshot.cpu().percent();
        if (cpuPercent > 85.0d) {
            penalties.add(new ScorePenaltyResponse(
                    PenaltyCode.CRITICAL_CPU_USAGE,
                    properties.getScore().getPenalties().getCriticalCpu(),
                    "CPU acima de 85%."
            ));
        } else if (cpuPercent > 75.0d) {
            penalties.add(new ScorePenaltyResponse(
                    PenaltyCode.HIGH_CPU_USAGE,
                    properties.getScore().getPenalties().getHighCpu(),
                    "CPU acima de 75%."
            ));
        }

        if (snapshot.memory().percent() > 80.0d) {
            penalties.add(new ScorePenaltyResponse(
                    PenaltyCode.HIGH_MEMORY_USAGE,
                    properties.getScore().getPenalties().getHighMemory(),
                    "Memoria acima de 80%."
            ));
        }
        return breakdown("RESOURCE", penalties);
    }
}
