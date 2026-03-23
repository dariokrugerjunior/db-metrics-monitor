package br.com.vivovaloriza.dbmetricsmonitor.dto;

public record DatabaseSettingResponse(
        String name,
        String setting,
        String unit
) {
}
