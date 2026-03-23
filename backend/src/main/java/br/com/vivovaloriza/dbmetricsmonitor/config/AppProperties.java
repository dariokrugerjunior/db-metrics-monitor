package br.com.vivovaloriza.dbmetricsmonitor.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Security security = new Security();
    private final Scheduler scheduler = new Scheduler();
    private final Monitoring monitoring = new Monitoring();
    private final History history = new History();
    private final Ai ai = new Ai();

    @Getter
    @Setter
    public static class Security {
        @NotBlank
        private String apiKey = "public-dev-key";
        @NotBlank
        private String headerName = "X-API-KEY";
        private boolean protectReadEndpoints = false;
    }

    @Getter
    @Setter
    public static class Scheduler {
        @NotBlank
        private String snapshotCron = "0 * * * * *";
    }

    @Getter
    @Setter
    public static class Monitoring {
        private int topQueryDefaultLimit = 20;
        private int runningQueryDefaultMinSeconds = 30;
    }

    @Getter
    @Setter
    public static class History {
        @NotBlank
        private String sqlitePath = "./data/db-metrics-history.db";
        private double cpuThresholdPercent = 60.0d;
        private double memoryThresholdPercent = 60.0d;
        private int recentLimit = 200;
    }

    @Getter
    @Setter
    public static class Ai {
        private boolean enabled = true;
        @NotBlank
        private String baseUrl = "https://api.openai.com/v1";
        @NotBlank
        private String model = "gpt-5-mini";
        private String apiKey = "";
        private int maxOutputTokens = 900;
    }
}
