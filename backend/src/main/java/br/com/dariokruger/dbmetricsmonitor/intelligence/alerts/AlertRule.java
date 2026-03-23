package br.com.dariokruger.dbmetricsmonitor.intelligence.alerts;

import br.com.dariokruger.dbmetricsmonitor.intelligence.dto.AlertItemResponse;
import java.util.List;

public interface AlertRule {

    List<AlertItemResponse> evaluate(AlertEvaluationContext context);
}
