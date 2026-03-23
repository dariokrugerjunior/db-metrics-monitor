package br.com.vivovaloriza.dbmetricsmonitor.service.prompt;

import br.com.vivovaloriza.dbmetricsmonitor.dto.DatabaseHealthSnapshot;
import br.com.vivovaloriza.dbmetricsmonitor.dto.HistoricalIncidentSummaryResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DatabaseHealthPromptBuilder {

    private final PromptInstructionBuilder promptInstructionBuilder;
    private final PromptContextBuilder promptContextBuilder;
    private final HealthSignalAnalyzer healthSignalAnalyzer;

    public DatabaseHealthPromptBuilder(
            PromptInstructionBuilder promptInstructionBuilder,
            PromptContextBuilder promptContextBuilder,
            HealthSignalAnalyzer healthSignalAnalyzer
    ) {
        this.promptInstructionBuilder = promptInstructionBuilder;
        this.promptContextBuilder = promptContextBuilder;
        this.healthSignalAnalyzer = healthSignalAnalyzer;
    }

    public String build(
            DatabaseHealthSnapshot snapshot,
            HistoricalIncidentSummaryResponse history,
            String userPrompt
    ) {
        DatabaseHealthSnapshot enrichedSnapshot = enrichSnapshot(snapshot, history);
        List<String> sections = List.of(
                buildRole(),
                buildInstructions(),
                buildSeverityRules(),
                buildOutputFormat(),
                buildContextData(enrichedSnapshot, history, userPrompt)
        );
        return String.join("\n\n", sections).strip() + "\n";
    }

    private DatabaseHealthSnapshot enrichSnapshot(
            DatabaseHealthSnapshot snapshot,
            HistoricalIncidentSummaryResponse history
    ) {
        return new DatabaseHealthSnapshot(
                snapshot.connections(),
                snapshot.locks(),
                snapshot.queries(),
                snapshot.cache(),
                snapshot.system(),
                healthSignalAnalyzer.analyze(snapshot, history),
                snapshot.timestamp()
        );
    }

    String buildRole() {
        return PromptFormattingUtils.section("ROLE", promptInstructionBuilder.buildRoleBlock());
    }

    String buildInstructions() {
        return PromptFormattingUtils.section("OBJECTIVE", promptInstructionBuilder.buildObjectiveBlock());
    }

    String buildSeverityRules() {
        return PromptFormattingUtils.section("RULES", String.join("\n\n", List.of(
                promptInstructionBuilder.buildRulesBlock(),
                "Regras explicitas de severidade:\n" + promptInstructionBuilder.buildSeverityRulesBlock()
        )));
    }

    String buildOutputFormat() {
        return PromptFormattingUtils.section("OUTPUT FORMAT", promptInstructionBuilder.buildOutputFormatBlock());
    }

    String buildSignals(DatabaseHealthSnapshot snapshot) {
        return "Sinais automaticos:\n" + promptContextBuilder.buildSignalsBlock(snapshot);
    }

    String buildDatabaseContext(DatabaseHealthSnapshot snapshot) {
        return String.join("\n\n", List.of(
                "Dados operacionais do banco:\n" + promptContextBuilder.buildDatabaseContext(snapshot),
                "Snapshot estruturado:\n```json\n" + promptContextBuilder.buildStructuredSnapshot(snapshot) + "\n```"
        ));
    }

    String buildHistoricalContext(HistoricalIncidentSummaryResponse history) {
        return "Historico:\n" + promptContextBuilder.buildHistoricalContext(history);
    }

    String buildUserContext(String userPrompt) {
        return "Contexto adicional do usuario:\n" + promptContextBuilder.buildUserContext(userPrompt);
    }

    private String buildContextData(
            DatabaseHealthSnapshot snapshot,
            HistoricalIncidentSummaryResponse history,
            String userPrompt
    ) {
        return PromptFormattingUtils.section("CONTEXT DATA", String.join("\n\n", List.of(
                buildSignals(snapshot),
                buildDatabaseContext(snapshot),
                buildHistoricalContext(history),
                buildUserContext(userPrompt)
        )));
    }
}
