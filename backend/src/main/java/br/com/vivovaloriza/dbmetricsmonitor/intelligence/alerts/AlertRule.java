package br.com.vivovaloriza.dbmetricsmonitor.intelligence.alerts;

import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.AlertItemResponse;
import java.util.List;

public interface AlertRule {

    List<AlertItemResponse> evaluate(AlertEvaluationContext context);
}
