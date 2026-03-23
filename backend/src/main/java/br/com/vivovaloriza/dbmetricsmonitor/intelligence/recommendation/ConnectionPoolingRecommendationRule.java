package br.com.vivovaloriza.dbmetricsmonitor.intelligence.recommendation;

import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.RecommendationItemResponse;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.enums.RecommendationCode;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.enums.RecommendationPriority;
import java.util.List;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(30)
public class ConnectionPoolingRecommendationRule implements RecommendationRule {

    @Override
    public Optional<RecommendationItemResponse> evaluate(RecommendationEvaluationContext context) {
        double usagePercent = context.snapshot().connections().usagePercent().doubleValue();
        if (usagePercent <= 70.0d || context.snapshot().derivedSignals().idleConnectionRatioPercent() < 40.0d) {
            return Optional.empty();
        }
        return Optional.of(new RecommendationItemResponse(
                RecommendationCode.REVIEW_CONNECTION_POOLING,
                "Revisar pool de conexao e reaproveitamento",
                "Uso alto de conexoes combinado com muitas sessoes ociosas indica pool superdimensionado ou conexoes pouco reaproveitadas.",
                RecommendationPriority.HIGH,
                "Quando a pressao por conexoes cresce sem a mesma proporcao de sessoes ativas, o problema costuma estar na configuracao do pool ou no ciclo de vida da sessao.",
                List.of(
                        "Valide max pool size, timeout e politica de recycle do pool.",
                        "Verifique sessoes idle por aplicacao para identificar origem dominante.",
                        "Confirme se conexoes sao devolvidas ao pool imediatamente apos uso."
                ),
                List.of("HIGH_CONNECTION_USAGE")
        ));
    }
}
