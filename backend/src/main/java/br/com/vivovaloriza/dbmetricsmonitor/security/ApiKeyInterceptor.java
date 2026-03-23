package br.com.vivovaloriza.dbmetricsmonitor.security;

import br.com.vivovaloriza.dbmetricsmonitor.config.AppProperties;
import br.com.vivovaloriza.dbmetricsmonitor.exception.ApiKeyUnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ApiKeyInterceptor implements HandlerInterceptor {

    private final AppProperties appProperties;

    public ApiKeyInterceptor(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!requiresApiKey(request)) {
            return true;
        }

        String providedApiKey = request.getHeader(appProperties.getSecurity().getHeaderName());
        if (!appProperties.getSecurity().getApiKey().equals(providedApiKey)) {
            throw new ApiKeyUnauthorizedException("API key ausente ou invalida.");
        }
        return true;
    }

    private boolean requiresApiKey(HttpServletRequest request) {
        if (!appProperties.getSecurity().isProtectReadEndpoints()) {
            return false;
        }

        String path = request.getRequestURI();
        String method = request.getMethod();
        if (path.startsWith("/api/v1/db/sessions/") && "POST".equalsIgnoreCase(method)) {
            return true;
        }
        if (path.equals("/api/v1/health")) {
            return false;
        }
        return appProperties.getSecurity().isProtectReadEndpoints();
    }
}
