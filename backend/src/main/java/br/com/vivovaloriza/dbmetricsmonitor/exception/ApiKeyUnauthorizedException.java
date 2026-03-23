package br.com.vivovaloriza.dbmetricsmonitor.exception;

public class ApiKeyUnauthorizedException extends RuntimeException {

    public ApiKeyUnauthorizedException(String message) {
        super(message);
    }
}
