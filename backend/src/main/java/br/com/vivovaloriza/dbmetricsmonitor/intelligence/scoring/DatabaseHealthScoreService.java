package br.com.vivovaloriza.dbmetricsmonitor.intelligence.scoring;

import br.com.vivovaloriza.dbmetricsmonitor.intelligence.config.DatabaseIntelligenceProperties;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.DatabaseHealthScoreResponse;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.DatabaseHealthSnapshot;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.ScoreBreakdownResponse;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.ScorePenaltyResponse;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.enums.HealthClassification;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DatabaseHealthScoreService {

    private final List<ScoreCategoryEvaluator> evaluators;
    private final DatabaseIntelligenceProperties properties;

    public DatabaseHealthScoreService(List<ScoreCategoryEvaluator> evaluators, DatabaseIntelligenceProperties properties) {
        this.evaluators = evaluators;
        this.properties = properties;
    }

    public DatabaseHealthScoreResponse calculate(DatabaseHealthSnapshot snapshot) {
        List<ScoreBreakdownResponse> breakdown = evaluators.stream()
                .map(evaluator -> evaluator.evaluate(snapshot))
                .toList();
        List<ScorePenaltyResponse> penalties = breakdown.stream()
                .flatMap(item -> item.penalties().stream())
                .toList();
        int score = Math.max(0, 100 - penalties.stream().mapToInt(ScorePenaltyResponse::points).sum());
        HealthClassification classification = classify(score);
        String summary = summarize(classification, penalties);

        log.info("db_intelligence_score_calculated collectedAt={} score={} classification={} penalties={}",
                snapshot.collectedAt(), score, classification, penalties.size());
        return new DatabaseHealthScoreResponse(score, classification, penalties, breakdown, summary);
    }

    private HealthClassification classify(int score) {
        if (score >= properties.getScore().getHealthyMinScore()) {
            return HealthClassification.HEALTHY;
        }
        if (score >= properties.getScore().getWarningMinScore()) {
            return HealthClassification.WARNING;
        }
        return HealthClassification.CRITICAL;
    }

    private String summarize(HealthClassification classification, List<ScorePenaltyResponse> penalties) {
        if (penalties.isEmpty()) {
            return "Ambiente estavel no snapshot atual, sem penalidades relevantes.";
        }
        String mainSignals = penalties.stream()
                .limit(2)
                .map(ScorePenaltyResponse::message)
                .reduce((left, right) -> left + " " + right)
                .orElse("Ha sinais operacionais relevantes.");
        return switch (classification) {
            case HEALTHY -> "Ambiente saudavel com atencoes pontuais. " + mainSignals;
            case WARNING -> "Ambiente em alerta moderado. " + mainSignals;
            case CRITICAL -> "Ambiente com risco operacional elevado. " + mainSignals;
        };
    }
}
