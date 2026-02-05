package com.christn.salesinventoryapi.config;

import com.christn.salesinventoryapi.auth.AuthUserDetails;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.List;

@TestConfiguration
public class SecurityTestConfig {

    public static void authenticateAs(
            Long id,
            String username,
            String... roles
    ) {
        List<SimpleGrantedAuthority> authorities = Arrays.stream(roles)
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                .map(SimpleGrantedAuthority::new)
                .toList();

        AuthUserDetails principal = new AuthUserDetails(id, username, "", true, authorities);

        Authentication auth =
                new UsernamePasswordAuthenticationToken(principal, null, authorities);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }
}
