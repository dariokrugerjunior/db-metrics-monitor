package br.com.vivovaloriza.dbmetricsmonitor.repository;

import br.com.vivovaloriza.dbmetricsmonitor.config.AppProperties;
import br.com.vivovaloriza.dbmetricsmonitor.dto.AiAnalysisHistoryResponse;
import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AiAnalysisHistoryRepository {

    private final AppProperties appProperties;

    @PostConstruct
    void initialize() throws Exception {
        Path path = sqlitePath();
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS ai_analysis_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        db_url_admin TEXT NOT NULL,
                        model TEXT NOT NULL,
                        user_prompt TEXT,
                        final_prompt TEXT NOT NULL,
                        analysis TEXT NOT NULL,
                        created_at TEXT NOT NULL
                    )
                    """);
        }
    }

    public void save(
            String dbUrlAdmin,
            String model,
            String userPrompt,
            String finalPrompt,
            String analysis,
            Instant createdAt
    ) {
        String sql = """
                INSERT INTO ai_analysis_history
                (db_url_admin, model, user_prompt, final_prompt, analysis, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, dbUrlAdmin);
            statement.setString(2, model);
            statement.setString(3, userPrompt);
            statement.setString(4, finalPrompt);
            statement.setString(5, analysis);
            statement.setString(6, createdAt.toString());
            statement.executeUpdate();
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao persistir histórico de análise IA.", ex);
        }
    }

    public List<AiAnalysisHistoryResponse> findByDbUrlAdmin(String dbUrlAdmin, int limit) {
        String sql = """
                SELECT id, db_url_admin, model, user_prompt, final_prompt, analysis, created_at
                FROM ai_analysis_history
                WHERE db_url_admin = ?
                ORDER BY datetime(created_at) DESC, id DESC
                LIMIT ?
                """;

        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, dbUrlAdmin);
            statement.setInt(2, limit);
            try (ResultSet rs = statement.executeQuery()) {
                List<AiAnalysisHistoryResponse> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(new AiAnalysisHistoryResponse(
                            rs.getLong("id"),
                            rs.getString("db_url_admin"),
                            rs.getString("model"),
                            rs.getString("user_prompt"),
                            rs.getString("final_prompt"),
                            rs.getString("analysis"),
                            Instant.parse(rs.getString("created_at"))
                    ));
                }
                return rows;
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao consultar histórico de análise IA.", ex);
        }
    }

    private Connection openConnection() throws Exception {
        return DriverManager.getConnection("jdbc:sqlite:" + sqlitePath());
    }

    private Path sqlitePath() {
        return Path.of(appProperties.getHistory().getSqlitePath()).toAbsolutePath();
    }
}
