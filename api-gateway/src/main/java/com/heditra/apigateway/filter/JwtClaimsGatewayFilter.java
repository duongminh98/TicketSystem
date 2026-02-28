package com.heditra.apigateway.filter;

import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtClaimsGatewayFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> securityContext.getAuthentication())
                .defaultIfEmpty(null)
                .flatMap(authentication -> {
                    ServerHttpRequest.Builder builder = exchange.getRequest().mutate();

                    if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                        Jwt jwt = jwtAuth.getToken();

                        String userId = jwt.getSubject();
                        Object preferred = jwt.getClaim("preferred_username");
                        String username = preferred != null ? preferred.toString() : userId;

                        List<String> roles = jwtAuth.getAuthorities().stream()
                                .map(a -> a.getAuthority())
                                .collect(Collectors.toList());

                        if (userId != null) {
                            builder.header("X-User-Id", userId);
                        }
                        if (username != null) {
                            builder.header("X-Username", username);
                        }
                        if (!roles.isEmpty()) {
                            builder.header("X-Roles", String.join(",", roles));
                        }
                    }

                    ServerWebExchange mutated = exchange.mutate()
                            .request(builder.build())
                            .build();

                    return chain.filter(mutated);
                });
    }

    @Override
    public int getOrder() {
        return -1;
    }
}

