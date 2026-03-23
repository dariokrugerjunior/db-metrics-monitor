package br.com.vivovaloriza.dbmetricsmonitor.intelligence.anomaly;

import br.com.vivovaloriza.dbmetricsmonitor.config.AppProperties;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.DatabaseHealthSnapshot;
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
public class DatabaseMetricsHistoryRepository {

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
                    CREATE TABLE IF NOT EXISTS operational_metric_snapshots (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        collected_at TEXT NOT NULL,
                        environment TEXT NOT NULL,
                        database_status TEXT NOT NULL,
                        total_connections INTEGER NOT NULL,
                        active_connections INTEGER NOT NULL,
                        idle_in_transaction_connections INTEGER NOT NULL,
                        blocked_locks INTEGER NOT NULL,
                        running_queries INTEGER NOT NULL,
                        long_running_queries INTEGER NOT NULL,
                        cache_hit_percent REAL NOT NULL,
                        cpu_percent REAL NOT NULL,
                        memory_percent REAL NOT NULL
                    )
                    """);
            statement.execute("CREATE INDEX IF NOT EXISTS idx_metric_snapshots_environment_time ON operational_metric_snapshots(environment, collected_at)");
        }
    }

    public void save(DatabaseHealthSnapshot snapshot) {
        String sql = """
                INSERT INTO operational_metric_snapshots (
                    collected_at,
                    environment,
                    database_status,
                    total_connections,
                    active_connections,
                    idle_in_transaction_connections,
                    blocked_locks,
                    running_queries,
                    long_running_queries,
                    cache_hit_percent,
                    cpu_percent,
                    memory_percent
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, snapshot.collectedAt().toString());
            statement.setString(2, snapshot.environment());
            statement.setString(3, snapshot.databaseStatus());
            statement.setInt(4, snapshot.connections().totalConnections());
            statement.setInt(5, snapshot.connections().activeConnections());
            statement.setInt(6, snapshot.connections().idleInTransactionConnections());
            statement.setInt(7, snapshot.locks().blockedLocks());
            statement.setInt(8, snapshot.runningQueries().runningQueries());
            statement.setInt(9, snapshot.runningQueries().longRunningQueries());
            statement.setDouble(10, snapshot.cache().cacheHitPercent().doubleValue());
            statement.setDouble(11, snapshot.cpu().percent());
            statement.setDouble(12, snapshot.memory().percent());
            statement.executeUpdate();
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao persistir baseline operacional.", ex);
        }
    }

    public List<HistoricalMetricsSnapshot> findByEnvironmentSince(String environment, Instant since, Instant untilExclusive) {
        String sql = """
                SELECT
                    collected_at,
                    total_connections,
                    active_connections,
                    idle_in_transaction_connections,
                    blocked_locks,
                    running_queries,
                    long_running_queries,
                    cache_hit_percent,
                    cpu_percent,
                    memory_percent
                FROM operational_metric_snapshots
                WHERE environment = ?
                  AND datetime(collected_at) >= datetime(?)
                  AND datetime(collected_at) < datetime(?)
                ORDER BY datetime(collected_at) ASC
                """;

        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, environment);
            statement.setString(2, since.toString());
            statement.setString(3, untilExclusive.toString());
            try (ResultSet rs = statement.executeQuery()) {
                List<HistoricalMetricsSnapshot> snapshots = new ArrayList<>();
                while (rs.next()) {
                    snapshots.add(new HistoricalMetricsSnapshot(
                            Instant.parse(rs.getString("collected_at")),
                            rs.getInt("total_connections"),
                            rs.getInt("active_connections"),
                            rs.getInt("idle_in_transaction_connections"),
                            rs.getInt("blocked_locks"),
                            rs.getInt("running_queries"),
                            rs.getInt("long_running_queries"),
                            rs.getDouble("cache_hit_percent"),
                            rs.getDouble("cpu_percent"),
                            rs.getDouble("memory_percent")
                    ));
                }
                return snapshots;
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao consultar baseline operacional.", ex);
        }
    }

    private Connection openConnection() throws Exception {
        return DriverManager.getConnection("jdbc:sqlite:" + sqlitePath());
    }

    private Path sqlitePath() {
        return Path.of(appProperties.getHistory().getSqlitePath()).toAbsolutePath();
    }
}
