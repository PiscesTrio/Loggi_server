package com.example.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The document's front matter, and the one thing springdoc cannot infer.
 *
 * <p>Authentication is invisible to a scanner: the JWT filter sits in the security chain, not
 * on the controllers, so nothing in the code says "these endpoints need a bearer token".
 * Without this the generated document describes an API that appears to need no credentials,
 * and every request tried from Swagger UI answers 401 with no hint why.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI loggiOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Loggi API")
                        .version("v1")
                        .description("""
                                Logistics management: warehouses and stock, commodities, \
                                delivery orders and their tracking, fleet and drivers, \
                                administrators and audit logs.

                                Every response is wrapped in an envelope of the shape \
                                `{ code, status, msg, data }`, where `code` repeats the HTTP \
                                status. `204 No Content` is the one exception and carries no \
                                body at all.

                                All endpoints require a bearer token except those under \
                                `/api/admin/login`, `/api/admin/init`, `/api/admin/hasInit` \
                                and `/api/admin/verification-code`, which have to work before \
                                a token exists.""")
                        // identifier, not just a name. OpenAPI 3.1 wants a SPDX identifier or a URL, and
                        // openapi-generator refuses to run without one - which is how this was
                        // found: the document looked fine in Swagger UI and failed validation the
                        // first time a machine tried to consume it.
                        .license(new License().name("MIT").identifier("MIT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                .components(new Components().addSecuritySchemes(BEARER,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("""
                                        Obtained from `POST /api/admin/login/password`. Send \
                                        it as `Authorization: Bearer <token>`.""")));
    }
}
