package com.banka1.user.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfiguration {
    @Bean
    public OpenAPI api() {
        return new OpenAPI()
                .info(new Info().title("Banka 1")
                        .description("User Service")
                        .version("0.0.1"))
                .addSecurityItem(
                    new SecurityRequirement().addList("jwt")
                )
                .components(
                    new Components().addSecuritySchemes("jwt",
                        new SecurityScheme().name("jwt").type(SecurityScheme.Type.HTTP).scheme("bearer"))
                );
    }
}
