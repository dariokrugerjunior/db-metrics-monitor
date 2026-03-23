package br.com.vivovaloriza.dbmetricsmonitor.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI dbMetricsOpenApi(AppProperties appProperties) {
        String schemeName = "ApiKeyAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("DB Metrics Monitor API")
                        .description("API para observabilidade operacional de PostgreSQL e da aplicacao Spring Boot.")
                        .version("1.0.0")
                        .contact(new Contact().name("Vivo Valoriza")))
                .components(new Components()
                        .addSecuritySchemes(schemeName, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name(appProperties.getSecurity().getHeaderName())))
                .addSecurityItem(new SecurityRequirement().addList(schemeName));
    }
}
