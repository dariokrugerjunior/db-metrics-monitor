package br.com.vivovaloriza.dbmetricsmonitor.service.prompt;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;
import java.util.StringJoiner;

public final class PromptFormattingUtils {

    private static final int QUERY_MAX_LENGTH = 180;

    private PromptFormattingUtils() {
    }

    public static String section(String title, String content) {
        return title + ":\n" + content.stripTrailing();
    }

    public static String bullets(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "- Nenhum dado relevante.";
        }

        StringJoiner joiner = new StringJoiner("\n");
        items.stream()
                .filter(item -> item != null && !item.isBlank())
                .forEach(item -> joiner.add("- " + item));
        String value = joiner.toString();
        return value.isBlank() ? "- Nenhum dado relevante." : value;
    }

    public static String labeledValue(String label, Object value) {
        return "- " + label + ": " + safe(value);
    }

    public static String safe(Object value) {
        if (value == null) {
            return "n/a";
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
        }
        return String.valueOf(value);
    }

    public static String compact(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", " ").trim();
    }

    public static String truncateQuery(String value) {
        String compacted = compact(value);
        if (compacted.length() <= QUERY_MAX_LENGTH) {
            return compacted;
        }
        return compacted.substring(0, QUERY_MAX_LENGTH - 3) + "...";
    }

    public static String formatDuration(Duration duration) {
        if (duration == null || duration.isNegative()) {
            return "0s";
        }

        long seconds = duration.toSeconds();
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;

        if (hours > 0) {
            return hours + "h " + minutes + "m " + remainingSeconds + "s";
        }
        if (minutes > 0) {
            return minutes + "m " + remainingSeconds + "s";
        }
        return remainingSeconds + "s";
    }

    public static BigDecimal round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    public static String formatSettingValue(String setting, String unit) {
        if (setting == null || setting.isBlank()) {
            return "n/a";
        }
        if (unit == null || unit.isBlank()) {
            return setting;
        }
        return setting + unit;
    }
}
