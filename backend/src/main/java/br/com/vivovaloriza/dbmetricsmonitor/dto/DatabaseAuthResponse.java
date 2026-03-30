package br.com.vivovaloriza.dbmetricsmonitor.dto;

/**
 * Resposta de autenticacao por banco de dados.
 * A senha nunca e incluida nesta resposta.
 */
public record DatabaseAuthResponse(
        boolean connected,
        String dbUrl,
        String databaseProductName,
        String databaseVersion,
        String currentDatabase,
        long responseTimeMs,
        String message
) {
}
