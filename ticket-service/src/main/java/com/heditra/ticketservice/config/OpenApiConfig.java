package com.heditra.ticketservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ticketServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ticket Service API")
                        .description("CQRS-based ticket booking microservice")
                        .version("1.0.0"));
    }
}
