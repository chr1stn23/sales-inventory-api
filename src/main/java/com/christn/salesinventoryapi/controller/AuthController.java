package com.christn.salesinventoryapi.controller;

import com.christn.salesinventoryapi.dto.request.LoginRequest;
import com.christn.salesinventoryapi.dto.request.RefreshRequest;
import com.christn.salesinventoryapi.dto.response.AuthResponse;
import com.christn.salesinventoryapi.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest http
    ) {
        return ResponseEntity.ok(
                authService.login(request, getIp(http), http.getHeader("User-Agent"))
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody RefreshRequest request,
            HttpServletRequest http
    ) {
        return ResponseEntity.ok(
                authService.refresh(request, getIp(http), http.getHeader("User-Agent"))
        );
    }

    private String getIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) return xf.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}