package com.heditra.apigateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsWebFilter;

import java.util.List;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("user-service", r -> r
                        .path("/users/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .addRequestHeader("X-Gateway-Request", "API-Gateway"))
                        .uri("lb://user-service"))
                .route("ticket-service", r -> r
                        .path("/tickets/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .addRequestHeader("X-Gateway-Request", "API-Gateway"))
                        .uri("lb://ticket-service"))
                .route("payment-service", r -> r
                        .path("/payments/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .addRequestHeader("X-Gateway-Request", "API-Gateway"))
                        .uri("lb://payment-service"))
                .route("notification-service", r -> r
                        .path("/notifications/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .addRequestHeader("X-Gateway-Request", "API-Gateway"))
                        .uri("lb://notification-service"))
                .route("inventory-service", r -> r
                        .path("/inventory/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .addRequestHeader("X-Gateway-Request", "API-Gateway"))
                        .uri("lb://inventory-service"))
                .build();
    }

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return new CorsWebFilter(source);
    }
}

