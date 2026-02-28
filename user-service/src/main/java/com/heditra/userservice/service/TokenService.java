package com.heditra.userservice.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class TokenService {

    public Long getUserIdFromToken() {
        Jwt jwt = getCurrentJwt();
        if (jwt == null) {
            return null;
        }
        String sub = jwt.getSubject();
        try {
            return sub != null ? Long.parseLong(sub) : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public String getUsernameFromToken() {
        Jwt jwt = getCurrentJwt();
        if (jwt == null) {
            return null;
        }
        Object preferred = jwt.getClaim("preferred_username");
        return preferred != null ? preferred.toString() : jwt.getSubject();
    }

    public List<String> getRolesFromToken() {
        Jwt jwt = getCurrentJwt();
        if (jwt == null) {
            return Collections.emptyList();
        }
        Object realmAccess = jwt.getClaim("realm_access");
        if (realmAccess instanceof java.util.Map<?, ?> map) {
            Object roles = map.get("roles");
            if (roles instanceof List<?> list) {
                return list.stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .toList();
            }
        }
        return Collections.emptyList();
    }

    private Jwt getCurrentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return null;
        }
        return jwt;
    }
}

