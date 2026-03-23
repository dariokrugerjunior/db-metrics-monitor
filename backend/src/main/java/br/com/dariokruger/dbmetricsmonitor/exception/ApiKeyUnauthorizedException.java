package br.com.dariokruger.dbmetricsmonitor.exception;

public class ApiKeyUnauthorizedException extends RuntimeException {

    public ApiKeyUnauthorizedException(String message) {
        super(message);
    }
}
