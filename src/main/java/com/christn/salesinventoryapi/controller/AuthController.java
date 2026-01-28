package com.christn.salesinventoryapi.controller;

import com.christn.salesinventoryapi.dto.request.LoginRequest;
import com.christn.salesinventoryapi.dto.response.AuthResponse;
import com.christn.salesinventoryapi.security.AuthUserDetails;
import com.christn.salesinventoryapi.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        var principal = (AuthUserDetails) authentication.getPrincipal();

        var roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        String token = jwtService.generateToken(
                principal.getUsername(),
                Map.of("userId", principal.getId(), "roles", roles)
        );

        return ResponseEntity.ok(new AuthResponse(token));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication, @RequestHeader("Authorization") String authorization) {
        String token = authorization.substring(7);
        Long userId = jwtService.extractClaim(token, c -> c.get("userId", Long.class));

        var roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return ResponseEntity.ok(Map.of(
                "id", userId,
                "email", authentication.getName(),
                "roles", roles
        ));
    }
}