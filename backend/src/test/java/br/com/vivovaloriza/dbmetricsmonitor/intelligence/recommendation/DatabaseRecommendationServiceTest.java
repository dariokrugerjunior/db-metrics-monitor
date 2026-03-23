package br.com.vivovaloriza.dbmetricsmonitor.intelligence.recommendation;

import br.com.vivovaloriza.dbmetricsmonitor.intelligence.DatabaseHealthSnapshotTestData;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.enums.RecommendationCode;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseRecommendationServiceTest {

    @Test
    void shouldRecommendInvestigatingOpenTransactionsWhenLocksAndIdleTransactionsCoexist() {
        DatabaseRecommendationService service = new DatabaseRecommendationService(List.of(new LockAndIdleRecommendationRule()));

        var response = service.generate(
                DatabaseHealthSnapshotTestData.snapshot(2, 1, 3, BigDecimal.valueOf(50), 5, BigDecimal.valueOf(99.5), 20, 30, 0, 0),
                List.of(),
                List.of()
        );

        assertThat(response.recommendations()).hasSize(1);
        assertThat(response.recommendations().getFirst().code()).isEqualTo(RecommendationCode.INVESTIGATE_OPEN_TRANSACTIONS);
    }
}
