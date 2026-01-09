package us.hogu.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import us.hogu.configuration.security.util.UserSVUtils;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configurazione estesa dello Swagger (solo per ambienti PROD e STAGING).
 * Aggiunge:
 * - Autenticazione JWT (bearer) richiesta globalmente.
 * - Header custom "Role-User" richiesto su ogni endpoint.
 */
@Profile({"dev"})
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
            // 1. Applica la sicurezza JWT a tutti gli endpoint
            .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
            // 2. Definisce i componenti riutilizzabili (schema di sicurezza e header)
            .components(new Components()
                // Definizione dello schema di sicurezza Bearer JWT
                .addSecuritySchemes(securitySchemeName,
                    new SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                )
            );
    }

}