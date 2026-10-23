package com.deadlocksimulator.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI deadlockSimulatorOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Self-Healing Deadlock Simulator API").version("1.0.0")
                        .description("Deadlock detection (WFG + three-colour DFS), three recovery strategies on "
                                + "isolated state copies, verification and comparative metrics."))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }
}
