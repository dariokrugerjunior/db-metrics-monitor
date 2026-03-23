package br.com.dariokruger.dbmetricsmonitor.intelligence.scoring;

import br.com.dariokruger.dbmetricsmonitor.intelligence.dto.DatabaseHealthSnapshot;
import br.com.dariokruger.dbmetricsmonitor.intelligence.dto.ScoreBreakdownResponse;

public interface ScoreCategoryEvaluator {

    ScoreBreakdownResponse evaluate(DatabaseHealthSnapshot snapshot);
}
