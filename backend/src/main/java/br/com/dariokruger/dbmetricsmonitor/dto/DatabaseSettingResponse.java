package br.com.dariokruger.dbmetricsmonitor.dto;

public record DatabaseSettingResponse(
        String name,
        String setting,
        String unit
) {
}
