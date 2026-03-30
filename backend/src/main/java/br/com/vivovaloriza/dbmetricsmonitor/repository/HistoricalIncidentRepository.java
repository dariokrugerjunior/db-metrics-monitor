package br.com.vivovaloriza.dbmetricsmonitor.repository;

import br.com.vivovaloriza.dbmetricsmonitor.config.AppProperties;
import br.com.vivovaloriza.dbmetricsmonitor.dto.HistoricalIncidentResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.HistoricalIncidentSummaryResponse;
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
public class HistoricalIncidentRepository {

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
                    CREATE TABLE IF NOT EXISTS historical_incidents (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        incident_type TEXT NOT NULL,
                        severity TEXT NOT NULL,
                        title TEXT NOT NULL,
                        details TEXT,
                        metric_value REAL,
                        metric_unit TEXT,
                        source TEXT,
                        reference_name TEXT,
                        created_at TEXT NOT NULL
                    )
                    """);
        }
    }

    public void saveIncident(
            String incidentType,
            String severity,
            String title,
            String details,
            Double metricValue,
            String metricUnit,
            String source,
            String referenceName,
            Instant createdAt
    ) {
        String sql = """
                INSERT INTO historical_incidents
                (incident_type, severity, title, details, metric_value, metric_unit, source, reference_name, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, incidentType);
            statement.setString(2, severity);
            statement.setString(3, title);
            statement.setString(4, details);
            if (metricValue == null) {
                statement.setNull(5, java.sql.Types.REAL);
            } else {
                statement.setDouble(5, metricValue);
            }
            statement.setString(6, metricUnit);
            statement.setString(7, source);
            statement.setString(8, referenceName);
            statement.setString(9, createdAt.toString());
            statement.executeUpdate();
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao persistir incidente historico.", ex);
        }
    }

    public List<HistoricalIncidentResponse> findRecentIncidents(int limit) {
        String sql = """
                SELECT id, incident_type, severity, title, details, metric_value, metric_unit, source, reference_name, created_at
                FROM historical_incidents
                ORDER BY datetime(created_at) DESC, id DESC
                LIMIT ?
                """;

        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet rs = statement.executeQuery()) {
                List<HistoricalIncidentResponse> incidents = new ArrayList<>();
                while (rs.next()) {
                    incidents.add(mapIncident(rs));
                }
                return incidents;
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao consultar incidentes historicos.", ex);
        }
    }

    public List<HistoricalIncidentResponse> findIncidentsPage(int limit, int offset) {
        String sql = """
                SELECT id, incident_type, severity, title, details, metric_value, metric_unit, source, reference_name, created_at
                FROM historical_incidents
                ORDER BY datetime(created_at) DESC, id DESC
                LIMIT ?
                OFFSET ?
                """;

        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            statement.setInt(2, offset);
            try (ResultSet rs = statement.executeQuery()) {
                List<HistoricalIncidentResponse> incidents = new ArrayList<>();
                while (rs.next()) {
                    incidents.add(mapIncident(rs));
                }
                return incidents;
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao paginar incidentes historicos.", ex);
        }
    }

    public long countIncidents() {
        String sql = "SELECT COUNT(*) AS total FROM historical_incidents";

        try (Connection connection = openConnection(); Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
            return rs.next() ? rs.getLong("total") : 0L;
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao contar incidentes historicos.", ex);
        }
    }

    public int deleteAllIncidents() {
        String sql = "DELETE FROM historical_incidents";

        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            return statement.executeUpdate(sql);
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao limpar incidentes historicos.", ex);
        }
    }

    public HistoricalIncidentSummaryResponse summarize() {
        return summarizeSince(null);
    }

    public HistoricalIncidentSummaryResponse summarizeSince(Instant since) {
        String sql = """
                SELECT
                    COUNT(*) AS total_incidents,
                    SUM(CASE WHEN incident_type = 'CPU_HIGH' THEN 1 ELSE 0 END) AS cpu_incidents,
                    SUM(CASE WHEN incident_type = 'MEMORY_HIGH' THEN 1 ELSE 0 END) AS memory_incidents,
                    SUM(CASE WHEN incident_type = 'LOCK_BLOCKING' THEN 1 ELSE 0 END) AS lock_incidents
                FROM historical_incidents
                WHERE (? IS NULL OR datetime(created_at) >= datetime(?))
                """;

        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            String cutoff = since == null ? null : since.toString();
            statement.setString(1, cutoff);
            statement.setString(2, cutoff);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return new HistoricalIncidentSummaryResponse(
                        rs.getInt("total_incidents"),
                        rs.getInt("cpu_incidents"),
                        rs.getInt("memory_incidents"),
                        rs.getInt("lock_incidents")
                );
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao resumir incidentes historicos.", ex);
        }
    }

    private HistoricalIncidentResponse mapIncident(ResultSet rs) throws Exception {
        Double metricValue = rs.getObject("metric_value") == null ? null : rs.getDouble("metric_value");
        return new HistoricalIncidentResponse(
                rs.getLong("id"),
                rs.getString("incident_type"),
                rs.getString("severity"),
                rs.getString("title"),
                rs.getString("details"),
                metricValue,
                rs.getString("metric_unit"),
                rs.getString("source"),
                rs.getString("reference_name"),
                Instant.parse(rs.getString("created_at"))
        );
    }

    private Connection openConnection() throws Exception {
        return DriverManager.getConnection("jdbc:sqlite:" + sqlitePath());
    }

    private Path sqlitePath() {
        return Path.of(appProperties.getHistory().getSqlitePath()).toAbsolutePath();
    }
}
