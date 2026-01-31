package com.christn.salesinventoryapi.controller;

import com.christn.salesinventoryapi.auth.AuthUserDetails;
import com.christn.salesinventoryapi.dto.request.LoginRequest;
import com.christn.salesinventoryapi.dto.request.LogoutRequest;
import com.christn.salesinventoryapi.dto.request.RefreshRequest;
import com.christn.salesinventoryapi.dto.response.AuthResponse;
import com.christn.salesinventoryapi.dto.response.MeResponse;
import com.christn.salesinventoryapi.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody LogoutRequest request,
            @AuthenticationPrincipal AuthUserDetails principal) {
        authService.logout(request, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("logout-all")
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal AuthUserDetails principal) {
        authService.logoutAll(principal);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal AuthUserDetails principal) {
        return ResponseEntity.ok(authService.me(principal));
    }

    private String getIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) return xf.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}