package com.trackit.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String JWT_SECURITY_SCHEME = "bearer-jwt";

    @Bean
    public OpenAPI trackItOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TrackIt API")
                        .description("REST API for TrackIt. In Swagger UI use **Authorize** → HTTP bearer and paste the JWT string from sign-in/signup (`token` field).")
                        .version("1.0"))
                .addSecurityItem(new SecurityRequirement().addList(JWT_SECURITY_SCHEME))
                .components(new Components().addSecuritySchemes(JWT_SECURITY_SCHEME,
                        new SecurityScheme()
                                .name(JWT_SECURITY_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT from `POST /api/auth/signin` or `POST /api/auth/signup` (`token` field).")));
    }
}
