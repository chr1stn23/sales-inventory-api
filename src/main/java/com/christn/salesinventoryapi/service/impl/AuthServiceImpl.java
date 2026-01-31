package com.christn.salesinventoryapi.service.impl;

import com.christn.salesinventoryapi.auth.AuthUserDetails;
import com.christn.salesinventoryapi.auth.JwtService;
import com.christn.salesinventoryapi.dto.request.LoginRequest;
import com.christn.salesinventoryapi.dto.request.LogoutRequest;
import com.christn.salesinventoryapi.dto.request.RefreshRequest;
import com.christn.salesinventoryapi.dto.response.AuthResponse;
import com.christn.salesinventoryapi.model.RefreshToken;
import com.christn.salesinventoryapi.model.Role;
import com.christn.salesinventoryapi.repository.RefreshTokenRepository;
import com.christn.salesinventoryapi.repository.UserRepository;
import com.christn.salesinventoryapi.service.AuthService;
import com.christn.salesinventoryapi.util.TokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, String ip, String userAgent) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        System.out.println("principal class=" + Objects.requireNonNull(authentication.getPrincipal()).getClass());
        var principal = (AuthUserDetails) authentication.getPrincipal();
        System.out.println("principal id=" + principal.getId());

        Long userId = principal.getId();
        String email = principal.getUsername();

        var roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).filter(Objects::nonNull)
                .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r)
                .collect(Collectors.toSet());

        String accessToken = jwtService.generateAccessToken(userId, email, roles);
        String refreshToken = TokenUtil.generateOpaqueToken();
        String refreshHash = TokenUtil.sha256Hex(refreshToken);

        var rt = RefreshToken.builder()
                .userId(userId)
                .tokenHash(refreshHash)
                .createdAt(LocalDateTime.now())
                .expiresAt(jwtService.refreshTokenExpiresAt())
                .ipAddress(ip)
                .userAgent(userAgent)
                .build();

        refreshTokenRepository.save(rt);

        return new AuthResponse(accessToken, refreshToken, "Bearer", jwtService.accessTokenExpiresInSeconds());
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshRequest request, String ip, String userAgent) {
        String providedHash = TokenUtil.sha256Hex(request.refreshToken());

        var stored = refreshTokenRepository.findByTokenHash(providedHash)
                .orElseThrow(() -> new RuntimeException("Refresh token invalido"));

        if (stored.isRevoked() || stored.isExpired()) {
            throw new RuntimeException("Refresh token expirado o revocado");
        }

        LocalDateTime now = LocalDateTime.now();

        stored.setRevokedAt(LocalDateTime.now());
        stored.setLastUsedAt(LocalDateTime.now());

        String newRefresh = TokenUtil.generateOpaqueToken();
        String newHash = TokenUtil.sha256Hex(newRefresh);

        var newStored = RefreshToken.builder()
                .userId(stored.getUserId())
                .tokenHash(newHash)
                .createdAt(now)
                .expiresAt(jwtService.refreshTokenExpiresAt())
                .ipAddress(ip)
                .userAgent(userAgent)
                .build();

        newStored = refreshTokenRepository.save(newStored);

        stored.setRevokedAt(now);
        stored.setLastUsedAt(now);
        stored.setReplacedByTokenId(newStored.getId());

        var user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String newAccess = jwtService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRoles().stream().map(Role::name).collect(Collectors.toSet())
        );

        return new AuthResponse(newAccess, newRefresh, "Bearer", jwtService.accessTokenExpiresInSeconds());
    }

    @Override
    public void logout(LogoutRequest request) {
        String hash = TokenUtil.sha256Hex(request.refreshToken());
        refreshTokenRepository.findByTokenHash((hash)).ifPresent(rt -> {
            rt.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(rt);
        });
    }
}
