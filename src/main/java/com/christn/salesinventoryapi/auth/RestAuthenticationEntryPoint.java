package com.christn.salesinventoryapi.auth;

import com.christn.salesinventoryapi.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            @NonNull AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        log.warn("Unauthorized error: {}", authException.getMessage());
        ApiError err = new ApiError(
                "https://christn.com/errors/401",
                "Unauthorized",
                HttpServletResponse.SC_UNAUTHORIZED,
                "No autenticado o token inválido",
                request.getRequestURI(),
                LocalDateTime.now()
        );

        objectMapper.writeValue(response.getOutputStream(), err);
    }
}
