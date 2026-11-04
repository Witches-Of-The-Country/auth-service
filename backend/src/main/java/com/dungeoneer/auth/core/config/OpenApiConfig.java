package com.dungeoneer.auth.core.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Dungeoneer - Auth Service API",
                version = "v1.0.0",
                description = "Authentication, Registration, and User Token Management"
        ),
        servers = {
                @Server(url = "http://localhost:8082", description = "Direct Auth Service (Port 8082)"),
                @Server(url = "http://localhost:9090", description = "API Gateway (Port 9090)")
        }
)
public class OpenApiConfig {
}
