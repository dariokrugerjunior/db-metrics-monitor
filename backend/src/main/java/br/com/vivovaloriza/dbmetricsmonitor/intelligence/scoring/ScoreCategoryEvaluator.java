package br.com.vivovaloriza.dbmetricsmonitor.intelligence.scoring;

import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.DatabaseHealthSnapshot;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.ScoreBreakdownResponse;

public interface ScoreCategoryEvaluator {

    ScoreBreakdownResponse evaluate(DatabaseHealthSnapshot snapshot);
}
