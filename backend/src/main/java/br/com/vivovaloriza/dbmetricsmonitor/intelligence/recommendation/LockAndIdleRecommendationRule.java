package br.com.vivovaloriza.dbmetricsmonitor.intelligence.recommendation;

import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.RecommendationItemResponse;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.enums.RecommendationCode;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.enums.RecommendationPriority;
import java.util.List;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class LockAndIdleRecommendationRule implements RecommendationRule {

    @Override
    public Optional<RecommendationItemResponse> evaluate(RecommendationEvaluationContext context) {
        if (context.snapshot().locks().blockedLocks() <= 0 || context.snapshot().connections().idleInTransactionConnections() <= 0) {
            return Optional.empty();
        }
        return Optional.of(new RecommendationItemResponse(
                RecommendationCode.INVESTIGATE_OPEN_TRANSACTIONS,
                "Investigar transacoes abertas e sessoes bloqueadoras",
                "Locks bloqueados e sessoes idle in transaction indicam transacoes abertas por tempo excessivo.",
                RecommendationPriority.URGENT,
                "A combinacao desses sinais sugere retencao de lock por sessoes que nao concluiram o ciclo transacional.",
                List.of(
                        "Mapeie a cadeia de bloqueio a partir da sessao raiz.",
                        "Revise timeouts de transacao e encerramento explicito no codigo da aplicacao.",
                        "Valide se ha rotinas mantendo transacoes abertas sem necessidade."
                ),
                List.of("LOCK_BLOCKED_SESSION", "IDLE_IN_TRANSACTION_DETECTED")
        ));
    }
}
