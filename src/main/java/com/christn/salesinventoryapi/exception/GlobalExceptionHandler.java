package com.christn.salesinventoryapi.exception;

import com.christn.salesinventoryapi.model.Role;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tools.jackson.databind.exc.InvalidFormatException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        String param = ex.getName();
        Object value = ex.getValue();
        Class<?> required = ex.getRequiredType();

        String expected = (required != null) ? required.getSimpleName() : "tipo válido";

        // Mensaje general
        String message = "Parámetro '" + param + "' inválido. Valor recibido: " + value +
                ". Se esperaba: " + expected + ".";

        // LocalDateTime
        if (required != null && required.equals(LocalDateTime.class)) {
            message = "Parámetro '" + param + "' inválido. Debe ser fecha-hora ISO-8601, ejemplo: 2026-02-14T10:00:00";
        }

        // Enums
        if (required != null && required.isEnum()) {
            String allowed = Arrays.stream(required.getEnumConstants())
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            message = "Parámetro '" + param + "' inválido. Valores permitidos: [" + allowed + "]";
        }

        log.warn("Invalid parameter: {}", message, ex);
        return buildError(message, HttpStatus.BAD_REQUEST, request.getRequestURI());
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiError> handleAuthorizationDenied(
            AuthorizationDeniedException ex,
            HttpServletRequest request
    ) {
        log.warn("Authorization denied: {}", ex.getMessage(), ex);
        return buildError(
                "No tienes permisos para este recurso",
                HttpStatus.FORBIDDEN,
                request.getRequestURI()
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthenticationException(
            AuthenticationException ex,
            HttpServletRequest request
    ) {
        String message = "Credenciales inválidas";

        if (ex instanceof DisabledException) {
            message = "Usuario deshabilitado";
        } else if (ex instanceof BadCredentialsException) {
            message = "Credenciales inválidas";
        } else if (ex instanceof LockedException) {
            message = "Usuario bloqueado";
        }

        log.warn("Authentication failed: {}", message, ex);
        return buildError(
                message,
                HttpStatus.UNAUTHORIZED,
                request.getRequestURI()
        );
    }


    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        Throwable cause = ex.getCause();

        // Jackson enum inválido
        if (cause instanceof InvalidFormatException ife) {

            // Si el tipo objetivo era Role
            Class<?> targetType = ife.getTargetType();
            if (targetType != null && targetType.equals(Role.class)) {
                String invalid = String.valueOf(ife.getValue());
                String allowed = java.util.Arrays.stream(Role.values())
                        .map(Enum::name)
                        .reduce((a, b) -> a + ", " + b)
                        .map(s -> "[" + s + "]")
                        .orElse("[ADMIN, SELLER, WAREHOUSE]");

                log.warn("Invalid enum value: {}", invalid);
                return buildError(
                        "Rol inválido: '" + invalid + "'. Valores permitidos: " + allowed,
                        HttpStatus.BAD_REQUEST,
                        request.getRequestURI()
                );
            }

            // Otro formato inválido cualquiera
            log.warn("Invalid JSON: {}", ex.getMessage(), ex);
            return buildError(
                    "JSON inválido: valor con formato incorrecto",
                    HttpStatus.BAD_REQUEST,
                    request.getRequestURI()
            );
        }

        // JSON mal formado o body ilegible
        log.warn("Invalid JSON: {}", ex.getMessage(), ex);
        return buildError(
                "JSON inválido o mal formado",
                HttpStatus.BAD_REQUEST,
                request.getRequestURI()
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        log.warn("Access denied: {}", ex.getMessage(), ex);
        return buildError(
                ex.getMessage(),
                HttpStatus.FORBIDDEN,
                request.getRequestURI()
        );
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> handleForbidden(
            ForbiddenException ex,
            HttpServletRequest request
    ) {
        log.warn("Forbidden: {}", ex.getMessage(), ex);
        return buildError(
                ex.getMessage(),
                HttpStatus.FORBIDDEN,
                request.getRequestURI()
        );
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiError> handleInsufficientStockError(
            InsufficientStockException ex,
            HttpServletRequest request
    ) {
        log.warn("Insufficient stock: {}", ex.getMessage(), ex);
        return buildError(
                ex.getMessage(),
                HttpStatus.CONFLICT,
                request.getRequestURI()
        );
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            EntityNotFoundException ex,
            HttpServletRequest request
    ) {
        log.warn("Entity not found: {}", ex.getMessage(), ex);
        return buildError(
                ex.getMessage(),
                HttpStatus.NOT_FOUND,
                request.getRequestURI()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        log.error("Business rule violated: {}", ex.getMessage(), ex);
        return buildError(
                ex.getMessage(),
                HttpStatus.BAD_REQUEST,
                request.getRequestURI()
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(
            IllegalStateException ex,
            HttpServletRequest request
    ) {
        log.error("Business rule violated: {}", ex.getMessage(), ex);
        return buildError(
                ex.getMessage(),
                HttpStatus.CONFLICT,
                request.getRequestURI()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        log.warn("Invalid data: {}", ex.getMessage(), ex);
        String detail = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .findFirst()
                .orElse("Datos inválidos");

        return buildError(
                detail,
                HttpStatus.BAD_REQUEST,
                request.getRequestURI()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleInternalServerError(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Internal server error: {}", ex.getMessage(), ex);
        return buildError(
                "Error interno del servidor",
                HttpStatus.INTERNAL_SERVER_ERROR,
                request.getRequestURI()
        );
    }

    private ResponseEntity<ApiError> buildError(
            String message,
            HttpStatus status,
            String path
    ) {
        ApiError error = new ApiError(
                "https://christn.com/errors/" + status.value(),
                status.getReasonPhrase(),
                status.value(),
                message,
                path,
                LocalDateTime.now()
        );

        return ResponseEntity.status(status).body(error);
    }
}
