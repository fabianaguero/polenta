package com.santec.polenta.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

/**
 * Basic OpenAPI configuration to enable Swagger UI documentation.
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Polenta MCP API",
        version = "1.0",
        description = "API for interacting with the Polenta MCP server"
    )
)
public class OpenApiConfig {
}

