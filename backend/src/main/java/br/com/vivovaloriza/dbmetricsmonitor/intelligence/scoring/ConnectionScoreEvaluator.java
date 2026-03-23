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
@Order(20)
public class ConnectionScoreEvaluator extends AbstractScoreCategoryEvaluator implements ScoreCategoryEvaluator {

    private final DatabaseIntelligenceProperties properties;

    public ConnectionScoreEvaluator(DatabaseIntelligenceProperties properties) {
        this.properties = properties;
    }

    @Override
    public ScoreBreakdownResponse evaluate(DatabaseHealthSnapshot snapshot) {
        List<ScorePenaltyResponse> penalties = new ArrayList<>();
        double usagePercent = snapshot.connections().usagePercent().doubleValue();
        if (usagePercent > 85.0d) {
            penalties.add(new ScorePenaltyResponse(
                    PenaltyCode.CRITICAL_CONNECTION_USAGE,
                    properties.getScore().getPenalties().getCriticalConnectionUsage(),
                    "Uso de conexoes acima de 85%."
            ));
        } else if (usagePercent > 70.0d) {
            penalties.add(new ScorePenaltyResponse(
                    PenaltyCode.HIGH_CONNECTION_USAGE,
                    properties.getScore().getPenalties().getHighConnectionUsage(),
                    "Uso de conexoes acima de 70%."
            ));
        }

        int idleInTransaction = snapshot.connections().idleInTransactionConnections();
        if (idleInTransaction > 5) {
            penalties.add(new ScorePenaltyResponse(
                    PenaltyCode.IDLE_IN_TRANSACTION_CRITICAL,
                    properties.getScore().getPenalties().getIdleInTransactionCritical(),
                    "Volume elevado de sessoes idle in transaction."
            ));
        } else if (idleInTransaction > 0) {
            penalties.add(new ScorePenaltyResponse(
                    PenaltyCode.IDLE_IN_TRANSACTION,
                    properties.getScore().getPenalties().getIdleInTransaction(),
                    "Existem sessoes idle in transaction."
            ));
        }
        return breakdown("CONNECTION", penalties);
    }
}
